package cn.bugstack.domain.activity.service.quota;

import cn.bugstack.domain.activity.model.aggregate.CreateOrderAggregate;
import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuStockService;
import cn.bugstack.domain.activity.service.quota.rule.IActionChain;
import cn.bugstack.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractRaffleActivityAccountQuota extends RaffleActivityAccountQuotaSupport implements IRaffleActivityAccountQuotaService, IRaffleActivitySkuStockService {

    public AbstractRaffleActivityAccountQuota(DefaultActivityChainFactory activityChainFactory, IActivityRepository activityRepository) {
        super(activityChainFactory, activityRepository);
    }

    // 构造器注入活动仓储

    @Override
    public String createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        // 1. 参数检验
        String userId = skuRechargeEntity.getUserId();
        Long sku = skuRechargeEntity.getSku();
        String outBusinessNo = skuRechargeEntity.getOutBusinessNo();
        if (null == sku || StringUtils.isBlank(userId) || StringUtils.isBlank(outBusinessNo)) {
            // 参数校验失败
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 2. 查询基础信息
        // 2.1 查询sku信息
        ActivitySkuEntity activitySkuEntity = queryActivitySku(sku);

        // 2.2 查询活动信息
        ActivityEntity activityEntity = queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());

        // 2.3 查询活动参与次数ActivityCount信息
        ActivityCountEntity activityCountEntity = queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());

        // 3. 基于查询到的基础信息进行规则链过滤
        IActionChain actionChain = activityChainFactory.openActionChain();
        Boolean success = actionChain.action(activityEntity, activitySkuEntity, activityCountEntity);

        // 4. 构建订单聚合对象
        CreateOrderAggregate createOrderAggregate = buildOrderAggregate(skuRechargeEntity, activityEntity, activitySkuEntity, activityCountEntity);

        // 5. 保存订单
        doSaveOrder(createOrderAggregate);

        // 6. 返回单号
        return createOrderAggregate.getActivityOrderEntity().getOrderId();
    }

    protected abstract void doSaveOrder(CreateOrderAggregate createOrderAggregate);

    protected abstract CreateOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivityEntity activityEntity, ActivitySkuEntity activitySkuEntity, ActivityCountEntity activityCountEntity);
}
