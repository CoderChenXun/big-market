package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.bugstack.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.bugstack.domain.rebate.model.entity.TaskEntity;
import cn.bugstack.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.bugstack.domain.rebate.model.valobj.DailyBehaviorRebateVo;
import cn.bugstack.domain.rebate.repository.IBehaviorRebateRepository;
import cn.bugstack.infrastructure.dao.IDailyBehaviorRebateDao;
import cn.bugstack.infrastructure.dao.ITaskDao;
import cn.bugstack.infrastructure.dao.IUserBehaviorRebateOrderDao;
import cn.bugstack.infrastructure.dao.po.DailyBehaviorRebate;
import cn.bugstack.infrastructure.dao.po.Task;
import cn.bugstack.infrastructure.dao.po.UserBehaviorRebateOrder;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class BehaviorRebateRepository implements IBehaviorRebateRepository {
    @Resource
    private IUserBehaviorRebateOrderDao userBehaviorRebateOrderDao;

    @Resource
    private IDailyBehaviorRebateDao dailyBehaviorRebateDao;

    @Resource
    private ITaskDao taskDao;

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private IDBRouterStrategy dbRouter;

    @Override
    public List<DailyBehaviorRebateVo> queryDailyBehaviorRebateByBehaviorType(BehaviorTypeVO behaviorType) {
        // 在主表查询
        DailyBehaviorRebate dailyBehaviorRebateReq = new DailyBehaviorRebate();
        dailyBehaviorRebateReq.setBehaviorType(behaviorType.getCode());
        List<DailyBehaviorRebate> dailyBehaviorRebateList = dailyBehaviorRebateDao.queryDailyBehaviorRebateByBehaviorType(dailyBehaviorRebateReq);
        // 转换
        List<DailyBehaviorRebateVo> dailyBehaviorRebateVoList = dailyBehaviorRebateList.stream().map(dailyBehaviorRebate -> {
                    return DailyBehaviorRebateVo.builder()
                            .behaviorType(dailyBehaviorRebate.getBehaviorType())
                            .rebateDesc(dailyBehaviorRebate.getRebateDesc())
                            .rebateType(dailyBehaviorRebate.getRebateType())
                            .rebateConfig(dailyBehaviorRebate.getRebateConfig())
                            .build();
                })
                .collect(Collectors.toList());
        return dailyBehaviorRebateVoList;
    }

    @Override
    public void saveBehaviorRebateAggregate(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregateList) {
        try {
            // 遍历聚合对象进行事务性保存
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    for (BehaviorRebateAggregate behaviorRebateAggregate : behaviorRebateAggregateList) {
                        BehaviorRebateOrderEntity behaviorRebateOrderEntity = behaviorRebateAggregate.getBehaviorRebateOrderEntity();
                        TaskEntity taskEntity = behaviorRebateAggregate.getTaskEntity();
                        // 用户返利记录对象
                        UserBehaviorRebateOrder userBehaviorRebateOrder = new UserBehaviorRebateOrder();
                        userBehaviorRebateOrder.setUserId(userId);
                        userBehaviorRebateOrder.setOrderId(behaviorRebateOrderEntity.getOrderId());
                        userBehaviorRebateOrder.setBehaviorType(behaviorRebateOrderEntity.getBehaviorType());
                        userBehaviorRebateOrder.setRebateDesc(behaviorRebateOrderEntity.getRebateDesc());
                        userBehaviorRebateOrder.setRebateType(behaviorRebateOrderEntity.getRebateType());
                        userBehaviorRebateOrder.setRebateConfig(behaviorRebateOrderEntity.getRebateConfig());
                        // 设置outBusinessNo
                        userBehaviorRebateOrder.setOutBusinessNo(behaviorRebateOrderEntity.getOutBusinessNo());
                        userBehaviorRebateOrder.setBizId(behaviorRebateOrderEntity.getBizId());
                        // 保存用户返利记录
                        userBehaviorRebateOrderDao.insert(userBehaviorRebateOrder);

                        // 任务对象
                        Task task = new Task();
                        task.setUserId(userId);
                        task.setTopic(taskEntity.getTopic());
                        task.setMessageId(taskEntity.getMessageId());
                        task.setMessage(JSON.toJSONString(taskEntity.getMessage()));
                        task.setState(taskEntity.getState().getCode());
                        // 保存任务对象
                        taskDao.insert(task);
                    }
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入返利记录失败 userId:{}", userId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }

        // 同步发送MQ消息
        for (BehaviorRebateAggregate behaviorRebateAggregate : behaviorRebateAggregateList){
            TaskEntity taskEntity = behaviorRebateAggregate.getTaskEntity();
            Task task = new Task();
            task.setUserId(userId);
            task.setMessageId(taskEntity.getMessageId());
            try {
                // 发送MQ消息，在事务外执行，如果失败还有事务补偿机制
                eventPublisher.publish(taskEntity.getTopic(), taskEntity.getMessage());
                taskDao.updateTaskSendMessageCompleted(task);
            } catch (Exception e) {
                log.error("写入返利记录 发送MQ消息失败 userId: {} topic: {}", taskEntity.getUserId(), taskEntity.getTopic());
                taskDao.updateTaskSendMessageFail(task);
            }
        }
    }

    @Override
    public List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo) {
        // 注意分库分表
        UserBehaviorRebateOrder userBehaviorRebateOrderReq = new UserBehaviorRebateOrder();
        userBehaviorRebateOrderReq.setUserId(userId);
        userBehaviorRebateOrderReq.setOutBusinessNo(outBusinessNo);

        List<UserBehaviorRebateOrder> userBehaviorRebateOrderList = userBehaviorRebateOrderDao.queryOrderByOutBusinessNo(userBehaviorRebateOrderReq);
        if (null == userBehaviorRebateOrderList) {
            return new ArrayList<>();
        }
        // 将实体类与Entity对象转换
        List<BehaviorRebateOrderEntity> behaviorRebateOrderEntityList = userBehaviorRebateOrderList.stream()
                .map(userBehaviorRebateOrder -> {
                    return BehaviorRebateOrderEntity.builder()
                            .userId(userBehaviorRebateOrder.getUserId())
                            .orderId(userBehaviorRebateOrder.getOrderId())
                            .behaviorType(userBehaviorRebateOrder.getBehaviorType())
                            .rebateDesc(userBehaviorRebateOrder.getRebateDesc())
                            .rebateType(userBehaviorRebateOrder.getRebateType())
                            .rebateConfig(userBehaviorRebateOrder.getRebateConfig())
                            .outBusinessNo(userBehaviorRebateOrder.getOutBusinessNo())
                            .bizId(userBehaviorRebateOrder.getBizId())
                            .build();
                })
                .collect(Collectors.toList());
        return behaviorRebateOrderEntityList;
    }
}
