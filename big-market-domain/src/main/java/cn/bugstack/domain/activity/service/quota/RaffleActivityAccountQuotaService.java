package cn.bugstack.domain.activity.service.quota;

import cn.bugstack.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.model.valobj.OrderStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class RaffleActivityAccountQuotaService extends AbstractRaffleActivityAccountQuota {

    public RaffleActivityAccountQuotaService(DefaultActivityChainFactory activityChainFactory, IActivityRepository activityRepository) {
        super(activityChainFactory, activityRepository);
    }


    @Override
    protected void doSaveOrder(CreateOrderAggregate createOrderAggregate) {
        // 保存订单对象
        activityRepository.doSaveOrder(createOrderAggregate);
    }

    @Override
    protected CreateOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivityEntity activityEntity, ActivitySkuEntity activitySkuEntity, ActivityCountEntity activityCountEntity) {
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
        activityOrderEntity.setState(OrderStateVO.completed);
        activityOrderEntity.setOutBusinessNo(skuRechargeEntity.getOutBusinessNo());

        // 构造聚合对象
        return CreateOrderAggregate.builder()
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
}
