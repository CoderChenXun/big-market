package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.activity.event.ActivitySkuStockZeroMessageEvent;
import cn.bugstack.domain.activity.model.aggregate.CreatePartakeOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.bugstack.domain.activity.model.valobj.UserRaffleOrderStateVo;
import cn.bugstack.infrastructure.event.EventPublisher;
import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.infrastructure.dao.*;
import cn.bugstack.infrastructure.dao.po.*;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.middleware.db.router.strategy.IDBRouterStrategy;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RLock;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

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

    @Resource
    private EventPublisher eventPublisher;

    @Resource
    private IUserRaffleOrderDao userRaffleOrderDao;

    @Resource
    private IUserCreditAccountDao userCreditAccountDao;

    @Resource
    private ActivitySkuStockZeroMessageEvent activitySkuStockZeroMessageEvent;

    @Resource
    private IRaffleActivityAccountMonthDao raffleActivityAccountMonthDao;

    @Resource
    private IRaffleActivityAccountDayDao raffleActivityAccountDayDao;


    @Override
    public ActivitySkuEntity queryActivitySku(Long sku) {
        RaffleActivitySku raffleActivitySku = raffleActivitySkuDao.queryActivitySku(sku);
        return ActivitySkuEntity.builder()
                .sku(raffleActivitySku.getSku())
                .activityId(raffleActivitySku.getActivityId())
                .activityCountId(raffleActivitySku.getActivityCountId())
                .stockCount(raffleActivitySku.getStockCount())
                .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                .payAmount(raffleActivitySku.getProductAmount())
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
                // 活动的状态
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
    public void doSaveNoPayOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_LOCK + createQuotaOrderAggregate.getUserId() + Constants.UNDERLINE + createQuotaOrderAggregate.getActivityId());
        try {
            // 锁
            lock.lock(3, TimeUnit.SECONDS);
            ActivityOrderEntity activityOrderEntity = createQuotaOrderAggregate.getActivityOrderEntity();
            // 1. 保存订单对象
            RaffleActivityOrder raffleActivityOrder = new RaffleActivityOrder();
            raffleActivityOrder.setUserId(createQuotaOrderAggregate.getUserId());
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
            // 设置
            raffleActivityOrder.setPayAmount(activityOrderEntity.getPayAmount());

            // 更新对象账户信息
            RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
            raffleActivityAccount.setUserId(createQuotaOrderAggregate.getUserId());
            raffleActivityAccount.setActivityId(activityOrderEntity.getActivityId());
            raffleActivityAccount.setTotalCount(activityOrderEntity.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(activityOrderEntity.getTotalCount());
            raffleActivityAccount.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityAccount.setDayCountSurplus(activityOrderEntity.getDayCount());
            raffleActivityAccount.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(activityOrderEntity.getMonthCount());

            // 更新对象月账户信息
            RaffleActivityAccountMonth raffleActivityAccountMonth = new RaffleActivityAccountMonth();
            raffleActivityAccountMonth.setUserId(createQuotaOrderAggregate.getUserId());
            raffleActivityAccountMonth.setActivityId(createQuotaOrderAggregate.getActivityId());
            raffleActivityAccountMonth.setMonth(RaffleActivityAccountMonth.currentMonth());
            raffleActivityAccountMonth.setMonthCount(activityOrderEntity.getMonthCount());
            raffleActivityAccountMonth.setMonthCountSurplus(activityOrderEntity.getMonthCount());

            // 更新对象日账户信息
            RaffleActivityAccountDay raffleActivityAccountDay = new RaffleActivityAccountDay();
            raffleActivityAccountDay.setUserId(createQuotaOrderAggregate.getUserId());
            raffleActivityAccountDay.setActivityId(createQuotaOrderAggregate.getActivityId());
            raffleActivityAccountDay.setDay(RaffleActivityAccountDay.currentDay());
            raffleActivityAccountDay.setDayCount(activityOrderEntity.getDayCount());
            raffleActivityAccountDay.setDayCountSurplus(activityOrderEntity.getDayCount());

            // 2. 在同一个领域内进行事务控制保证事实一致性，在不同领域内实现最终一致性
            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createQuotaOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 3. 保存订单消息
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    // 4. 更新账户信息
                    RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryAccountByUserId(raffleActivityAccount);
                    // 5. 如果count == 0，则说明账户不存在，则创建账户
                    if (null == raffleActivityAccountRes) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    } else {
                        // 账户存在，则更新账户信息
                        raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    }

                    // 更新账户信息-月
                    raffleActivityAccountMonthDao.addAccountQuota(raffleActivityAccountMonth);
                    // 6. 更新账户信息-日
                    raffleActivityAccountDayDao.addAccountQuota(raffleActivityAccountDay);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode());
                }
            });
        } finally {
            dbRouter.clear();
            // 解锁
            lock.unlock();
        }
    }

    @Override
    public void doSaveCreditPayOrder(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        // 先保存抽奖活动订单RaffleActivityOrder消息
        try {
            ActivityOrderEntity activityOrderEntity = createQuotaOrderAggregate.getActivityOrderEntity();
            // 1. 保存订单对象
            RaffleActivityOrder raffleActivityOrder = new RaffleActivityOrder();
            raffleActivityOrder.setUserId(createQuotaOrderAggregate.getUserId());
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
            // 设置
            raffleActivityOrder.setPayAmount(activityOrderEntity.getPayAmount());

            // 2. 在同一个领域内进行事务控制保证事实一致性，在不同领域内实现最终一致性
            // 以用户ID作为切分键，通过 doRouter 设定路由【这样就保证了下面的操作，都是同一个链接下，也就保证了事务的特性】
            dbRouter.doRouter(createQuotaOrderAggregate.getUserId());
            // 编程式事务
            transactionTemplate.execute(status -> {
                try {
                    // 3. 保存订单消息
                    raffleActivityOrderDao.insert(raffleActivityOrder);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入订单记录，唯一索引冲突 userId: {} activityId: {} sku: {}", activityOrderEntity.getUserId(), activityOrderEntity.getActivityId(), activityOrderEntity.getSku(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode());
                }
            });
        } finally {
            dbRouter.clear();
        }
    }

    @Override
    public void cacheActivitySkuStockCount(String cachedKey, Integer stockCount) {
        // 1. 首先查询缓存中是否存在
        if (redisService.isExists(cachedKey)) {
            return;
        }
        redisService.setAtomicLong(cachedKey, stockCount);
    }

    @Override
    public boolean subtractionActivitySkuStock(Long sku, String cachedKey, Date endDateTime) {
        long surplus = redisService.decr(cachedKey);
        if (surplus == 0) {
            // todo：库存减为0，发送消息给MQ，进行库存清空，同时清空redis的阻塞队列
            eventPublisher.publish(activitySkuStockZeroMessageEvent.topic(), activitySkuStockZeroMessageEvent.buildEventMessage(sku));
        } else if (surplus < 0) {
            // 库存小于0，库存恢复为0
            redisService.setAtomicLong(cachedKey, 0);
            return false;
        }

        // 库存大于0，给当前的线程加Nx锁，防止超卖
        String lockKey = cachedKey + Constants.UNDERLINE + surplus;
        long expireMillis = endDateTime.getTime() - System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1);
        Boolean setNx = redisService.setNx(lockKey, expireMillis, TimeUnit.MILLISECONDS);
        if (!setNx) {
            log.info("活动sku库存加锁失败 {}", lockKey);
        }
        return setNx;
    }

    @Override
    public void activitySkuStockConsumeSendQueue(ActivitySkuStockKeyVO activitySkuStockKeyVO) {
        // 1. 生产sku库存扣减消息
        String cachedKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<Object> blockingQueue = redisService.getBlockingQueue(cachedKey);
        RDelayedQueue<Object> delayedQueue = redisService.getDelayedQueue(blockingQueue);
        delayedQueue.offer(activitySkuStockKeyVO, 3, TimeUnit.SECONDS);
    }

    @Override
    public ActivitySkuStockKeyVO takeQueueValue() {
        String cachedKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> blockingQueue = redisService.getBlockingQueue(cachedKey);
        return blockingQueue.poll();
    }

    @Override
    public void updateSkuStock(Long sku) {
        raffleActivitySkuDao.updateSkuStock(sku);
    }

    @Override
    public void clearActivitySkuStock(Long sku) {
        raffleActivitySkuDao.clearActivitySkuStock(sku);
    }

    @Override
    public void clearQueueValue() {
        String cachedKey = Constants.RedisKey.ACTIVITY_SKU_COUNT_QUERY_KEY;
        RBlockingQueue<ActivitySkuStockKeyVO> blockingQueue = redisService.getBlockingQueue(cachedKey);
        blockingQueue.clear();
    }

    @Override
    public UserRaffleOrderEntity queryNoUsedRaffleOrder(PartakeRaffleActivityEntity partakeRaffleActivityEntity) {
        // 注意分库分表
        UserRaffleOrder userRaffleOrderReq = new UserRaffleOrder();
        userRaffleOrderReq.setUserId(partakeRaffleActivityEntity.getUserId());
        userRaffleOrderReq.setActivityId(partakeRaffleActivityEntity.getActivityId());
        UserRaffleOrder userRaffleOrder = userRaffleOrderDao.queryNoUsedRaffleOrder(userRaffleOrderReq);
        if (null == userRaffleOrder) {
            return null;
        }
        // 查询活动的结束时间
        ActivityEntity activity = queryRaffleActivityByActivityId(partakeRaffleActivityEntity.getActivityId());

        return UserRaffleOrderEntity.builder()
                .userId(userRaffleOrder.getUserId())
                .activityId(userRaffleOrder.getActivityId())
                .activityName(userRaffleOrder.getActivityName())
                .strategyId(userRaffleOrder.getStrategyId())
                .orderId(userRaffleOrder.getOrderId())
                .orderTime(userRaffleOrder.getOrderTime())
                .orderState(UserRaffleOrderStateVo.valueOf(userRaffleOrder.getOrderState().toUpperCase()))
                .endDateTime(activity.getEndDateTime())
                .build();
    }

    @Override
    public void saveCreatePartakeOrderAggregate(CreatePartakeOrderAggregate createPartakeOrderAggregate) {
        // 保存聚合对象：用户活动总账户信息、用户活动账户月信息、用户活动账户日信息、用户活动订单信息
        try {
            String userId = createPartakeOrderAggregate.getUserId();
            Long activityId = createPartakeOrderAggregate.getActivityId();
            ActivityAccountEntity activityAccountEntity = createPartakeOrderAggregate.getActivityAccountEntity();
            ActivityAccountMonthEntity activityAccountMonthEntity = createPartakeOrderAggregate.getActivityAccountMonthEntity();
            ActivityAccountDayEntity activityAccountDayEntity = createPartakeOrderAggregate.getActivityAccountDayEntity();
            UserRaffleOrderEntity userRaffleOrderEntity = createPartakeOrderAggregate.getUserRaffleOrderEntity();

            // 首先进行统一的分库分表，根据userId进行事务操作
            dbRouter.doRouter(userId);
            transactionTemplate.execute(status -> {
                try {
                    // 1. 更新用户活动总账户信息
                    int totalCount = raffleActivityAccountDao.updateActivityAccountSubtractionQuota(RaffleActivityAccount.builder()
                            .userId(userId)
                            .activityId(activityId)
                            .build());
                    if (1 != totalCount) {
                        // 回滚
                        status.setRollbackOnly();
                        log.info("更新用户活动总账户信息失败 userId:{} activityId:{} totalCount:{}", userId, activityId, totalCount);
                        throw new AppException(ResponseCode.ACCOUNT_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_QUOTA_ERROR.getInfo());
                    }

                    // 2. 更新或插入用户活动账户月信息
                    if (createPartakeOrderAggregate.isExistAccountMonth()) {
                        // 更新用户活动账户月信息
                        int totalMonthCount = raffleActivityAccountMonthDao.updateActivityAccountMonthSubtractionQuota(
                                RaffleActivityAccountMonth.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .month(activityAccountMonthEntity.getMonth())
                                        .build()
                        );
                        if (1 != totalMonthCount) {
                            // 回滚
                            status.setRollbackOnly();
                            log.info("更新用户活动账户月信息失败 userId:{} activityId:{} totalMonthCount:{}", userId, activityId, totalMonthCount);
                            throw new AppException(ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_MONTH_QUOTA_ERROR.getInfo());
                        }
                    } else {
                        // 插入用户活动账户月信息
                        raffleActivityAccountMonthDao.insertActivityAccountMonth(
                                RaffleActivityAccountMonth.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .month(activityAccountMonthEntity.getMonth())
                                        .monthCount(activityAccountMonthEntity.getMonthCount())
                                        .monthCountSurplus(activityAccountEntity.getMonthCountSurplus() - 1)
                                        .build()
                        );
                        // 新创建用户月账户，则更新总账户月镜像额度
                        raffleActivityAccountDao.updateActivityAccountMonthSurplusImageQuota(
                                RaffleActivityAccount.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .monthCountSurplus(activityAccountEntity.getMonthCountSurplus() - 1)
                                        .build()
                        );
                    }

                    // 3. 插入用户活动账户日信息
                    if (createPartakeOrderAggregate.isExistAccountDay()) {
                        int totalDayCount = raffleActivityAccountDayDao.updateActivityAccountDaySubtractionQuota(
                                RaffleActivityAccountDay.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .day(activityAccountDayEntity.getDay())
                                        .build()
                        );
                        if (1 != totalDayCount) {
                            // 回滚
                            status.setRollbackOnly();
                            log.info("更新用户活动账户日信息失败 userId:{} activityId:{} totalDayCount:{}", userId, activityId, totalDayCount);
                            throw new AppException(ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getCode(), ResponseCode.ACCOUNT_DAY_QUOTA_ERROR.getInfo());
                        }
                    } else {
                        // 插入用户活动账户日信息
                        raffleActivityAccountDayDao.insertActivityAccountDay(
                                RaffleActivityAccountDay.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .day(activityAccountDayEntity.getDay())
                                        .dayCount(activityAccountDayEntity.getDayCount())
                                        .dayCountSurplus(activityAccountEntity.getDayCountSurplus() - 1)
                                        .build()
                        );
                        // 新创建用户日账户，则更新总账户日镜像额度
                        raffleActivityAccountDao.updateActivityAccountDaySurplusImageQuota(
                                RaffleActivityAccount.builder()
                                        .userId(userId)
                                        .activityId(activityId)
                                        .dayCountSurplus(activityAccountEntity.getDayCountSurplus() - 1)
                                        .build()
                        );
                    }

                    // 写入活动参与订单raffleOrder
                    userRaffleOrderDao.insert(
                            UserRaffleOrder.builder()
                                    .userId(userId)
                                    .activityId(activityId)
                                    .activityName(userRaffleOrderEntity.getActivityName())
                                    .strategyId(userRaffleOrderEntity.getStrategyId())
                                    .orderId(userRaffleOrderEntity.getOrderId())
                                    .orderTime(userRaffleOrderEntity.getOrderTime())
                                    .orderState(userRaffleOrderEntity.getOrderState().getCode())
                                    .build()
                    );

                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("写入创建参与活动记录，唯一索引冲突 userId: {} activityId: {}", userId, activityId, e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
        }
    }

    @Override
    public ActivityAccountEntity queryActivityAccountByUserIdAndActivityId(String userId, Long activityId) {
        RaffleActivityAccount raffleActivityAccountReq = new RaffleActivityAccount();
        raffleActivityAccountReq.setUserId(userId);
        raffleActivityAccountReq.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserIdAndActivityId(raffleActivityAccountReq);
        if (raffleActivityAccountRes == null) {
            return null;
        }
        return ActivityAccountEntity.builder()
                .userId(raffleActivityAccountRes.getUserId())
                .activityId(raffleActivityAccountRes.getActivityId())
                .totalCount(raffleActivityAccountRes.getTotalCount())
                .totalCountSurplus(raffleActivityAccountRes.getTotalCountSurplus())
                .dayCount(raffleActivityAccountRes.getDayCount())
                .dayCountSurplus(raffleActivityAccountRes.getDayCountSurplus())
                .monthCount(raffleActivityAccountRes.getMonthCount())
                .monthCountSurplus(raffleActivityAccountRes.getMonthCountSurplus())
                .build();
    }

    @Override
    public ActivityAccountMonthEntity queryActivityAccountMonthByUserId(String userId, Long activityId, String month) {
        // 注意分库分表
        RaffleActivityAccountMonth raffleActivityAccountMonthReq = new RaffleActivityAccountMonth();
        raffleActivityAccountMonthReq.setUserId(userId);
        raffleActivityAccountMonthReq.setActivityId(activityId);
        raffleActivityAccountMonthReq.setMonth(month);
        RaffleActivityAccountMonth raffleActivityAccountMonthRes = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(raffleActivityAccountMonthReq);
        if (raffleActivityAccountMonthRes == null) {
            return null;
        }
        return ActivityAccountMonthEntity.builder()
                .userId(raffleActivityAccountMonthRes.getUserId())
                .activityId(raffleActivityAccountMonthRes.getActivityId())
                .month(raffleActivityAccountMonthRes.getMonth())
                .monthCount(raffleActivityAccountMonthRes.getMonthCount())
                .monthCountSurplus(raffleActivityAccountMonthRes.getMonthCountSurplus())
                .build();
    }

    @Override
    public ActivityAccountDayEntity queryActivityAccountDayByUserId(String userId, Long activityId, String day) {
        // 注意分库分表
        RaffleActivityAccountDay raffleActivityAccountDayReq = new RaffleActivityAccountDay();
        raffleActivityAccountDayReq.setUserId(userId);
        raffleActivityAccountDayReq.setActivityId(activityId);
        raffleActivityAccountDayReq.setDay(day);
        RaffleActivityAccountDay raffleActivityAccountDayRes = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayReq);
        if (raffleActivityAccountDayRes == null) {
            return null;
        }
        return ActivityAccountDayEntity.builder()
                .userId(raffleActivityAccountDayRes.getUserId())
                .activityId(raffleActivityAccountDayRes.getActivityId())
                .day(raffleActivityAccountDayRes.getDay())
                .dayCount(raffleActivityAccountDayRes.getDayCount())
                .dayCountSurplus(raffleActivityAccountDayRes.getDayCountSurplus())
                .build();
    }

    @Override
    public List<ActivitySkuEntity> queryActivitySkuByActivityId(Long activityId) {
        RaffleActivitySku raffleActivitySkuReq = new RaffleActivitySku();
        raffleActivitySkuReq.setActivityId(activityId);
        List<RaffleActivitySku> raffleActivitySkuRes = raffleActivitySkuDao.queryActivitySkuByActivityId(raffleActivitySkuReq);
        if (null == raffleActivitySkuRes || raffleActivitySkuRes.isEmpty()) {
            return null;
        }
        // 将实体类转换为Entity对象
        List<ActivitySkuEntity> activitySkuEntityList = raffleActivitySkuRes.stream()
                .map(raffleActivitySku -> {
                    return ActivitySkuEntity.builder()
                            .sku(raffleActivitySku.getSku())
                            .activityId(raffleActivitySku.getActivityId())
                            .activityCountId(raffleActivitySku.getActivityCountId())
                            .stockCount(raffleActivitySku.getStockCount())
                            .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                            .build();
                })
                .collect(Collectors.toList());
        return activitySkuEntityList;
    }


    @Override
    public ActivityAccountEntity queryUserActivityAccountEntity(String userId, Long activityId) {
        // 注意分库
        ActivityAccountEntity activityAccountEntity = new ActivityAccountEntity();
        // 查询用户活动总账户
        RaffleActivityAccount raffleActivityAccountReq = new RaffleActivityAccount();
        raffleActivityAccountReq.setUserId(userId);
        raffleActivityAccountReq.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserIdAndActivityId(raffleActivityAccountReq);
        // 如果活动总账户不存在
        if (raffleActivityAccountRes == null) {
            return ActivityAccountEntity.builder()
                    .userId(userId)
                    .activityId(activityId)
                    .totalCount(0)
                    .totalCountSurplus(0)
                    .dayCount(0)
                    .dayCountSurplus(0)
                    .monthCount(0)
                    .monthCountSurplus(0)
                    .build();
        }
        activityAccountEntity.setUserId(raffleActivityAccountRes.getUserId());
        activityAccountEntity.setActivityId(raffleActivityAccountRes.getActivityId());
        activityAccountEntity.setTotalCount(raffleActivityAccountRes.getTotalCount());
        activityAccountEntity.setTotalCountSurplus(raffleActivityAccountRes.getTotalCountSurplus());

        // 查询用户活动账户日
        RaffleActivityAccountDay raffleActivityAccountDayReq = new RaffleActivityAccountDay();
        raffleActivityAccountDayReq.setUserId(userId);
        raffleActivityAccountDayReq.setActivityId(activityId);
        // TODO xfg未添加这个字段
        raffleActivityAccountDayReq.setDay(RaffleActivityAccountDay.currentDay());
        RaffleActivityAccountDay raffleActivityAccountDayRes = raffleActivityAccountDayDao.queryActivityAccountDayByUserId(raffleActivityAccountDayReq);

        // 查询用户活动账户月
        RaffleActivityAccountMonth raffleActivityAccountMonthReq = new RaffleActivityAccountMonth();
        raffleActivityAccountMonthReq.setUserId(userId);
        raffleActivityAccountMonthReq.setActivityId(activityId);
        // TODO xfg未添加这个字段
        raffleActivityAccountMonthReq.setMonth(RaffleActivityAccountMonth.currentMonth());
        RaffleActivityAccountMonth raffleActivityAccountMonthRes = raffleActivityAccountMonthDao.queryActivityAccountMonthByUserId(raffleActivityAccountMonthReq);

        if (null == raffleActivityAccountMonthRes) {
            activityAccountEntity.setMonthCount(raffleActivityAccountRes.getMonthCount());
            activityAccountEntity.setMonthCountSurplus(raffleActivityAccountRes.getMonthCountSurplus());
        } else {
            activityAccountEntity.setMonthCount(raffleActivityAccountMonthRes.getMonthCount());
            activityAccountEntity.setMonthCountSurplus(raffleActivityAccountMonthRes.getMonthCountSurplus());
        }

        if (null == raffleActivityAccountDayRes) {
            activityAccountEntity.setDayCount(raffleActivityAccountRes.getDayCount());
            activityAccountEntity.setDayCountSurplus(raffleActivityAccountRes.getDayCountSurplus());
        } else {
            activityAccountEntity.setDayCount(raffleActivityAccountDayRes.getDayCount());
            activityAccountEntity.setDayCountSurplus(raffleActivityAccountDayRes.getDayCountSurplus());
        }
        return activityAccountEntity;
    }

    @Override
    public Integer queryActivityAccountTotalUseCount(String userId, Long activityId) {
        // 注意分库
        RaffleActivityAccount raffleActivityAccountReq = new RaffleActivityAccount();
        raffleActivityAccountReq.setUserId(userId);
        raffleActivityAccountReq.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserIdAndActivityId(raffleActivityAccountReq);
        return null == raffleActivityAccountRes ? 0 : raffleActivityAccountRes.getTotalCount() - raffleActivityAccountRes.getTotalCountSurplus();
    }

    @Override
    public Integer queryActivityAccountPartakeCount(String userId, Long activityId) {
        // 注意分库
        RaffleActivityAccount raffleActivityAccountReq = new RaffleActivityAccount();
        raffleActivityAccountReq.setUserId(userId);
        raffleActivityAccountReq.setActivityId(activityId);
        RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryActivityAccountByUserIdAndActivityId(raffleActivityAccountReq);
        return null == raffleActivityAccountRes ? 0 : raffleActivityAccountRes.getTotalCount() - raffleActivityAccountRes.getTotalCountSurplus();
    }

    @Override
    public void updateOrder(DeliveryOrderEntity deliveryOrderEntity) {
        // 分布式锁
        RLock lock = redisService.getLock(Constants.RedisKey.ACTIVITY_ACCOUNT_UPDATE_LOCK + deliveryOrderEntity.getUserId());
        try {
            lock.lock(3, TimeUnit.SECONDS);

            // 1. 查询RaffleActivityOrder
            RaffleActivityOrder raffleActivityOrderReq = new RaffleActivityOrder();
            raffleActivityOrderReq.setUserId(deliveryOrderEntity.getUserId());
            raffleActivityOrderReq.setOutBusinessNo(deliveryOrderEntity.getOutBusinessNo());
            RaffleActivityOrder raffleActivityOrderRes = raffleActivityOrderDao.queryRaffleActivityOrder(raffleActivityOrderReq);

            // 1.1 如果未查询到活动订单，解锁
            if (null == raffleActivityOrderRes) {
                if (lock.isLocked()) {
                    lock.unlock();
                }
                return;
            }

            // 2. 更新活动总账户
            RaffleActivityAccount raffleActivityAccount = new RaffleActivityAccount();
            raffleActivityAccount.setUserId(raffleActivityOrderRes.getUserId());
            raffleActivityAccount.setActivityId(raffleActivityOrderRes.getActivityId());
            // 更新抽奖总次数
            raffleActivityAccount.setTotalCount(raffleActivityOrderRes.getTotalCount());
            raffleActivityAccount.setTotalCountSurplus(raffleActivityOrderRes.getTotalCount());
            // 更新抽奖日次数
            raffleActivityAccount.setDayCount(raffleActivityOrderRes.getDayCount());
            raffleActivityAccount.setDayCountSurplus(raffleActivityOrderRes.getDayCount());
            // 更新抽奖月次数
            raffleActivityAccount.setMonthCount(raffleActivityOrderRes.getMonthCount());
            raffleActivityAccount.setMonthCountSurplus(raffleActivityOrderRes.getMonthCount());

            // 3. 更新活动账户日
            RaffleActivityAccountDay raffleActivityAccountDay = new RaffleActivityAccountDay();
            raffleActivityAccountDay.setUserId(raffleActivityOrderRes.getUserId());
            raffleActivityAccountDay.setActivityId(raffleActivityOrderRes.getActivityId());
            raffleActivityAccountDay.setDay(RaffleActivityAccountDay.currentDay());
            raffleActivityAccountDay.setDayCount(raffleActivityOrderRes.getDayCount());
            raffleActivityAccountDay.setDayCountSurplus(raffleActivityOrderRes.getDayCount());

            // 3. 更新活动账户月
            RaffleActivityAccountMonth raffleActivityAccountMonth = new RaffleActivityAccountMonth();
            raffleActivityAccountMonth.setUserId(raffleActivityOrderRes.getUserId());
            raffleActivityAccountMonth.setActivityId(raffleActivityOrderRes.getActivityId());
            raffleActivityAccountMonth.setMonth(RaffleActivityAccountMonth.currentMonth());
            raffleActivityAccountMonth.setMonthCount(raffleActivityOrderRes.getMonthCount());
            raffleActivityAccountMonth.setMonthCountSurplus(raffleActivityOrderRes.getMonthCount());

            // 分库分表
            dbRouter.doRouter(deliveryOrderEntity.getUserId());
            // 编程型事务
            transactionTemplate.execute(status -> {
                try {
                    // 4. 更新raffleActivityOrder订单状态
                    int count = raffleActivityOrderDao.updateOrderCompleted(raffleActivityOrderReq);
                    if (1 != count) {
                        status.setRollbackOnly();
                        return 1;
                    }
                    // 4.1 查询活动总账户
                    RaffleActivityAccount raffleActivityAccountRes = raffleActivityAccountDao.queryAccountByUserId(raffleActivityAccount);
                    if (null == raffleActivityAccountRes) {
                        raffleActivityAccountDao.insert(raffleActivityAccount);
                    } else {
                        raffleActivityAccountDao.updateAccountQuota(raffleActivityAccount);
                    }
                    // 4.2 更新活动账户日
                    raffleActivityAccountDayDao.addAccountQuota(raffleActivityAccountDay);
                    // 4.3 更新活动账户月
                    raffleActivityAccountMonthDao.addAccountQuota(raffleActivityAccountMonth);
                    return 1;
                } catch (DuplicateKeyException e) {
                    status.setRollbackOnly();
                    log.error("更新订单记录，完成态，唯一索引冲突 userId: {} outBusinessNo: {}", deliveryOrderEntity.getUserId(), deliveryOrderEntity.getOutBusinessNo(), e);
                    throw new AppException(ResponseCode.INDEX_DUP.getCode(), e);
                }
            });
        } finally {
            dbRouter.clear();
            if (lock.isLocked()) {
                lock.unlock();
            }
        }
    }

    @Override
    public UnpaidActivityOrderEntity queryUnpaidActivityOrder(SkuRechargeEntity skuRechargeEntity) {
        String userId = skuRechargeEntity.getUserId();
        Long sku = skuRechargeEntity.getSku();
        // 组装查询条件
        RaffleActivityOrder raffleActivityOrderReq = new RaffleActivityOrder();
        raffleActivityOrderReq.setUserId(userId);
        raffleActivityOrderReq.setSku(sku);
        // 查询
        RaffleActivityOrder raffleActivityOrderRes = raffleActivityOrderDao.queryUnpaidActivityOrder(raffleActivityOrderReq);
        if (null == raffleActivityOrderRes) {
            return null;
        }
        // 组装返回结果
        UnpaidActivityOrderEntity unpaidActivityOrderEntity = UnpaidActivityOrderEntity.builder()
                .userId(raffleActivityOrderRes.getUserId())
                .orderId(raffleActivityOrderRes.getOrderId())
                .outBusinessNo(raffleActivityOrderRes.getOutBusinessNo())
                .payAmount(raffleActivityOrderRes.getPayAmount())
                .build();
        return unpaidActivityOrderEntity;
    }

    @Override
    public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
        // 查询sku信息和activityCount信息进行组装
        // 1. 查询sku信息
        RaffleActivitySku raffleActivitySkuReq = new RaffleActivitySku();
        raffleActivitySkuReq.setActivityId(activityId);
        List<RaffleActivitySku> raffleActivitySkuList = raffleActivitySkuDao.queryActivitySkuByActivityId(raffleActivitySkuReq);
        if (null == raffleActivitySkuList) {
            return null;
        }
        List<SkuProductEntity> skuProductEntityList = raffleActivitySkuList.stream().map(raffleActivitySku -> {
            return SkuProductEntity.builder()
                    .sku(raffleActivitySku.getSku())
                    .activityId(raffleActivitySku.getActivityId())
                    .activityCountId(raffleActivitySku.getActivityCountId())
                    .stockCount(raffleActivitySku.getStockCount())
                    .stockCountSurplus(raffleActivitySku.getStockCountSurplus())
                    .payAmount(raffleActivitySku.getProductAmount())
                    .build();
        }).collect(Collectors.toList());
        // 2. 联合查询activityCount信息
        for (SkuProductEntity skuProductEntity : skuProductEntityList) {
            RaffleActivityCount raffleActivityCountRes = raffleActivityCountDao.queryRaffleActivityCountByActivityCountId(skuProductEntity.getActivityCountId());
            SkuProductEntity.ActivityCount activityCount = SkuProductEntity.ActivityCount.builder()
                    .totalCount(raffleActivityCountRes.getTotalCount())
                    .dayCount(raffleActivityCountRes.getDayCount())
                    .monthCount(raffleActivityCountRes.getMonthCount())
                    .build();
            skuProductEntity.setActivityCount(activityCount);
        }
        return skuProductEntityList;
    }

    @Override
    public BigDecimal queryUserCreditAccountAmount(String userId) {
        UserCreditAccount userCreditAccountReq = new UserCreditAccount();
        userCreditAccountReq.setUserId(userId);
        // 注意分库分表
        try {
            dbRouter.doRouter(userId);
            UserCreditAccount userCreditAccountRes = userCreditAccountDao.queryUserCreditAccount(userCreditAccountReq);
            return null == userCreditAccountRes ? BigDecimal.ZERO : userCreditAccountRes.getAvailableAmount();
        } finally {
            dbRouter.clear();
        }
    }
}
