package cn.bugstack.domain.activity.service.quota;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuProductService;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import cn.bugstack.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class RaffleActivityAccountQuotaService extends AbstractRaffleActivityAccountQuota implements IRaffleActivitySkuProductService {


    public RaffleActivityAccountQuotaService(DefaultActivityChainFactory activityChainFactory, IActivityRepository activityRepository, Map<String, ITradePolicy> tradePolicyGroup) {
        super(activityChainFactory, activityRepository, tradePolicyGroup);
    }

    @Override
    protected CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivityEntity activityEntity, ActivitySkuEntity activitySkuEntity, ActivityCountEntity activityCountEntity) {
        // 1. 订单实体对象
        ActivityOrderEntity activityOrderEntity = new ActivityOrderEntity();
        activityOrderEntity.setUserId(skuRechargeEntity.getUserId());
        activityOrderEntity.setSku(activitySkuEntity.getSku());
        activityOrderEntity.setActivityId(activityEntity.getActivityId());
        activityOrderEntity.setActivityName(activityEntity.getActivityName());
        // 从活动实体对象中获取策略ID
        activityOrderEntity.setStrategyId(activityEntity.getStrategyId());
        // 公司里一般会有专门的雪花算法UUID服务，我们这里直接生成个12位就可以了
        activityOrderEntity.setOrderId(RandomStringUtils.randomNumeric(12));
        activityOrderEntity.setOrderTime(new Date());
        activityOrderEntity.setTotalCount(activityCountEntity.getTotalCount());
        activityOrderEntity.setDayCount(activityCountEntity.getDayCount());
        activityOrderEntity.setMonthCount(activityCountEntity.getMonthCount());
        activityOrderEntity.setOutBusinessNo(skuRechargeEntity.getOutBusinessNo());
        // 设置payAmount
        activityOrderEntity.setPayAmount(activitySkuEntity.getPayAmount());

        // 构造聚合对象
        return CreateQuotaOrderAggregate.builder()
                .userId(skuRechargeEntity.getUserId())
                .activityId(activityEntity.getActivityId())
                .totalCount(activityCountEntity.getTotalCount())
                .dayCount(activityCountEntity.getDayCount())
                .monthCount(activityCountEntity.getMonthCount())
                .activityOrderEntity(activityOrderEntity)
                .build();
    }

    @Override
    public ActivitySkuStockKeyVO takeQueueValue() {
        // 读取阻塞队列的生产者数据，并返回
        ActivitySkuStockKeyVO activitySkuStockKeyVO = activityRepository.takeQueueValue();
        return activitySkuStockKeyVO;
    }

    @Override
    public void updateSkuStock(Long sku) {
        // 根据sku更新库存信息
        activityRepository.updateSkuStock(sku);
    }

    @Override
    public void clearActivitySkuStock(Long sku) {
        // 清空活动sku库存信息
        activityRepository.clearActivitySkuStock(sku);
    }

    @Override
    public void clearQueueValue() {
        // 清空阻塞队列信息
        activityRepository.clearQueueValue();
    }


    @Override
    public ActivityAccountEntity queryUserActivityAccountEntity(String userId, Long activityId) {
        return activityRepository.queryUserActivityAccountEntity(userId, activityId);
    }

    @Override
    public Integer queryUserActivityAccountTotalUseCount(String userId, Long activityId) {
        return activityRepository.queryActivityAccountTotalUseCount(userId, activityId);
    }

    @Override
    public Integer queryRaffleActivityAccountPartakeCount(String userId, Long activityId) {
        return activityRepository.queryActivityAccountPartakeCount(userId, activityId);
    }

    @Override
    public void updateOrder(DeliveryOrderEntity deliveryOrderEntity) {
        // 当订单完成时，更新订单信息
        activityRepository.updateOrder(deliveryOrderEntity);
    }

    @Override
    public List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId) {
        return activityRepository.querySkuProductEntityListByActivityId(activityId);
    }
}
