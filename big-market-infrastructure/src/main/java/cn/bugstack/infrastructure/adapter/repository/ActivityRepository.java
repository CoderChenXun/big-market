package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.infrastructure.dao.*;
import cn.bugstack.infrastructure.dao.po.*;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;

@Slf4j
@Repository
public class ActivityRepository implements IActivityRepository {
    @Resource
    private IRedisService redisService;
    @Resource
    private IRaffleActivitySkuDao raffleActivitySkuDao;

    @Resource
    private IRaffleActivityDao raffleActivityDao;

    @Resource
    private IRaffleActivityCountDao raffleActivityCountDao;

    @Resource
    private IRaffleActivityOrderDao raffleActivityOrderDao;

    @Resource
    private IRaffleActivityAccountDao raffleActivityAccountDao;

    @Resource
    private TransactionTemplate transactionTemplate;

    @Resource
    private IDBRouterStrategy dbRouter;


    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        RaffleActivitySku raffleActivitySku = raffleActivitySkuDao.queryActivitySku(sku);
        return ActivitySkuEntity.builder()
                .sku(raffleActivitySku.getSku())
                .activityId(raffleActivitySku.getActivityId())
                .activityCountId(raffleActivitySku.getActivityCountId())
                .stockCount(raffleActivitySku.getStockCount())
                .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                .build();
    }

    @Override
    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        // 1. 优先从缓存中获取
        String cachedKey = Constants.RedisKey.ACTIVITY_KEY + activityId;
        ActivityEntity activityEntity = redisService.getValue(cachedKey);
        if (activityEntity != null) {
            return activityEntity;
        }
        // 2. 从数据库中获取
        RaffleActivity raffleActivity = raffleActivityDao.queryRaffleActivityByActivityId(activityId);
        activityEntity = ActivityEntity.builder()
                .activityId(raffleActivity.getActivityId())
                .activityName(raffleActivity.getActivityName())
                .activityDesc(raffleActivity.getActivityDesc())
                .beginDateTime(raffleActivity.getBeginDateTime())
                .endDateTime(raffleActivity.getEndDateTime())
                .strategyId(raffleActivity.getStrategyId())
                .state(ActivityStateVO.valueOf(raffleActivity.getState()))
                .build();
        // 3. 写入数据到缓存中
        redisService.setValue(cachedKey, activityEntity);
        return activityEntity;
    }

    @Override
    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        // 1. 优先从缓存中获取
        String cachedKey = Constants.RedisKey.ACTIVITY_COUNT_KEY + activityCountId;
        ActivityCountEntity activityCountEntity = redisService.getValue(cachedKey);
        if (activityCountEntity != null) {
            return activityCountEntity;
        }

        // 2. 从数据库中获取
        RaffleActivityCount raffleActivityCount = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(activityCountId);
        activityCountEntity = ActivityCountEntity.builder()
                .activityCountId(raffleActivityCount.getActivityCountId())
                .totalCount(raffleActivityCount.getTotalCount())
                .dayCount(raffleActivityCount.getDayCount())
                .monthCount(raffleActivityCount.getMonthCount())
                .build();

        // 3. 写入数据到缓存中
        redisService.setValue(cachedKey, activityCountEntity);
        return activityCountEntity;
    }

    @Override
    public void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        try {
            ActivityOrderEntity activityOrderEntity = createOrderAggregate.getActivityOrderEntity();
            // 1. 保存订单对象
            RaffleActivityOrder raffleActivityOrder = new RaffleActivityOrder();
            raffleActivityOrder.setUserId(createOrderAggregate.getUserId());
            // sku信息
            raffleActivityOrder.setSku(activityOrderEntity.getSku());
            raffleActivityOrder.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityOrder.setActivityName(activityOrderEntity.getActivityName());
            raffleActivityOrder.setStrategyId(activityOrderEntity.getStrategyId());
            raffleActivityOrder.setOrderId(activityOrderEntity.getOrderId());
            raffleActivityOrder.setOrderTime(activityOrderEntity.getOrderTime());
            // 添加的次数信息
            raffleActivityOrder.setTotalCount(activityOrderEntity.getTotalCount());
            raffleActivityOrder.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityOrder.setMonthCount(activityOrderEntity.getMonthCount());
            // 设置状态
            raffleActivityOrder.setState(activityOrderEntity.getState().getCode());
            // 外部业务号，保证业务的幂等性
            raffleActivityOrder.setOutBusinessNo(activityOrderEntity.getOutBusinessNo());

            // 更新账户对象
            RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
            raffleActivityAccount.setUserId(createOrderAggregate.getUserId());
            raffleActivityAccount.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityAccount.setTotalCount(activityOrderEntity.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(activityOrderEntity.getTotalCount());
            raffleActivityAccount.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityAccount.setDayCountSurplus(activityOrderEntity.getDayCount());
            raffleActivityAccount.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(activityOrderEntity.getMonthCount());

            // 2. 在同一个领域内进行事务控制保证事实一致性，在不同领域内实现最终一致性
            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 3. 保存订单消息
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    // 4. 更新账户信息
                    int count = raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    // 5. 如果count == 0，则说明账户不存在，则创建账户
                    if (count == 0) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    }
                    return 1;
                }catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode());
                }
            });
        }finally {
            dbRouter.clear();
        }
    }
}
