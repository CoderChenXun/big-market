package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.credit.model.aggregate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.entity.TaskEntity;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import cn.bugstack.domain.credit.repository.ICreditRepository;
import cn.bugstack.domain.rebate.model.valobj.TaskStateVO;
import cn.bugstack.infrastructure.dao.ITaskDao;
import cn.bugstack.infrastructure.dao.IUserCreditAccountDao;
import cn.bugstack.infrastructure.dao.IUserCreditOrderDao;
import cn.bugstack.infrastructure.dao.po.Task;
import cn.bugstack.infrastructure.dao.po.UserCreditAccount;
import cn.bugstack.infrastructure.dao.po.UserCreditOrder;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.common.Constants;
import com.esotericsoftware.minlog.Log;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 16:03
 **/
@Slf4j
@Repository
public class CreditRepository implements ICreditRepository {
    @Resource
    private IRedisService redisService;

    @Resource
    private IUserCreditAccountDao userCreditAccountDao;

    @Resource
    private IUserCreditOrderDao userCreditOrderDao;

    @Resource
    private IDBRouterStrategy dbRouter;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private ITaskDao taskDao;


    @Override
    public void saveUserCreditTradeOrder(TradeAggregate tradeAggregate) {
        String userId = tradeAggregate.getUserId();
        CreditAccountEntity creditAccountEntity = tradeAggregate.getCreditAccountEntity();
        CreditOrderEntity creditOrderEntity = tradeAggregate.getCreditOrderEntity();
        TaskEntity taskEntity = tradeAggregate.getTaskEntity();

        // 组装积分账户对象
        UserCreditAccount userCreditAccountReq = new UserCreditAccount();
        userCreditAccountReq.setUserId(userId);
        userCreditAccountReq.setTotalAmount(creditAccountEntity.getAdjustAmount());
        userCreditAccountReq.setAvailableAmount(creditAccountEntity.getAdjustAmount());

        // 组装用户积分订单对象
        UserCreditOrder userCreditOrderReq = new UserCreditOrder();
        userCreditOrderReq.setUserId(userId);
        userCreditOrderReq.setOrderId(creditOrderEntity.getOrderId());
        userCreditOrderReq.setTradeName(creditOrderEntity.getTradeName().getName());
        userCreditOrderReq.setTradeType(creditOrderEntity.getTradeType().getCode());
        userCreditOrderReq.setTradeAmount(creditOrderEntity.getTradeAmount());
        userCreditOrderReq.setOutBusinessNo(creditOrderEntity.getOutBusinessNo());

        Task task = new Task();
        task.setUserId(taskEntity.getUserId());
        task.setTopic(taskEntity.getTopic());
        task.setMessageId(taskEntity.getMessageId());
        task.setMessage(taskEntity.getMessageId());
        task.setState(TaskStateVO.create.getCode());

        // 注意分库分表
        RLock lock = redisService.getLock(Constants.RedisKey.USER_CREDIT_ACCOUNT_LOCK + userId + Constants.UNDERLINE + creditOrderEntity.getOutBusinessNo());
        try {
            lock.lock(3, TimeUnit.SECONDS);
            // 分库分表
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 1. 首先查询积分账户是否存在
                    UserCreditAccount userCreditAccount = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
                    if (null == userCreditAccount) {
                        // 2. 插入积分账户
                        userCreditAccountDao.insert(userCreditAccountReq);
                    } else {
                        // 3. 更新积分账户
                        userCreditAccountDao.updateAddAmount(userCreditAccountReq);
                    }
                    // 2. 插入用户积分订单
                    userCreditOrderDao.insert(userCreditOrderReq);
                    // 3. 插入任务实体
                    taskDao.insert(task);
                } catch (DuplicateKeyException e) {
                    log.error("调整账户积分额度异常，唯一索引冲突 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
                    status.setRollbackOnly();
                } catch (Exception e) {
                    log.info("调整账户积分额度异常 userId:{} orderId:{}", userId, creditOrderEntity.getOrderId(), e);
                    status.setRollbackOnly();
                }
                return 1;
            });
        } finally {
            dbRouter.clear();
            // 解锁
            lock.unlock();
        }

        // 只有在积分支付时才需要发送MQ消息
        if(TradeTypeVO.REVERSE.equals(creditOrderEntity.getTradeType())){
            try {
                eventPublisher.publish(taskEntity.getTopic(), taskEntity.getMessage());
                // 更新Task状态
                taskDao.updateTaskSendMessageCompleted(task);
                log.info("调整账户积分记录，发送MQ消息完成 userId: {} orderId:{} topic: {}", userId, creditOrderEntity.getOrderId(), task.getTopic());
            }catch(Exception e){
                log.error("调整账户积分记录，发送MQ消息失败 userId: {} topic: {}", userId, task.getTopic());
                taskDao.updateTaskSendMessageFail(task);
            }
        }
    }
}
