package cn.bugstack.domain.activity.service.quota;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.entity.*;
import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuStockService;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import cn.bugstack.domain.activity.service.quota.rule.IActionChain;
import cn.bugstack.domain.activity.service.quota.rule.factory.DefaultActivityChainFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.math.BigDecimal;
import java.util.Map;

@Slf4j
public abstract class AbstractRaffleActivityAccountQuota extends RaffleActivityAccountQuotaSupport implements IRaffleActivityAccountQuotaService, IRaffleActivitySkuStockService {

    private Map<String, ITradePolicy> tradePolicyGroup;

    // 构造器注入活动仓储
    public AbstractRaffleActivityAccountQuota(DefaultActivityChainFactory activityChainFactory, IActivityRepository activityRepository, Map<String, ITradePolicy> tradePolicyGroup) {
        super(activityChainFactory, activityRepository);
        this.tradePolicyGroup = tradePolicyGroup;
    }

    @Override
    public UnpaidActivityOrderEntity createSkuRechargeOrder(SkuRechargeEntity skuRechargeEntity) {
        // 1. 参数检验
        String userId = skuRechargeEntity.getUserId();
        Long sku = skuRechargeEntity.getSku();
        String outBusinessNo = skuRechargeEntity.getOutBusinessNo();
        if (null == sku || StringUtils.isBlank(userId) || StringUtils.isBlank(outBusinessNo)) {
            // 参数校验失败
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 1.1 查询是否存在等待支付的订单
        if(OrderTradeTypeVO.credit_pay_trade.equals(skuRechargeEntity.getTradeType())){
            UnpaidActivityOrderEntity unpaidActivityOrderEntity = activityRepository.queryUnpaidActivityOrder(skuRechargeEntity);
            if (null != unpaidActivityOrderEntity) {
                return unpaidActivityOrderEntity;
            }
        }
        // 2. 查询基础信息
        // 2.1 查询sku信息
        ActivitySkuEntity activitySkuEntity = queryActivitySku(sku);

        // 2.2 查询活动信息
        ActivityEntity activityEntity = queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());

        // 2.3 查询活动参与次数ActivityCount信息
        ActivityCountEntity activityCountEntity = queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());

        // 2.4 账户额度 【交易属性的兑换，需要校验额度账户】,比如说积分兑换sku，需要查询用户积分额度
        if(OrderTradeTypeVO.credit_pay_trade.equals(skuRechargeEntity.getTradeType())){
            BigDecimal availableAmount = activityRepository.queryUserCreditAccountAmount(userId);
            if(availableAmount.compareTo(activitySkuEntity.getPayAmount()) < 0){
                throw new AppException(ResponseCode.USER_CREDIT_ACCOUNT_NO_AVAILABLE_AMOUNT.getCode(), ResponseCode.USER_CREDIT_ACCOUNT_NO_AVAILABLE_AMOUNT.getInfo());
            }
        }

        // 3. 基于查询到的基础信息进行规则链过滤，同时趋势扣减sku库存
        IActionChain actionChain = activityChainFactory.openActionChain();
        Boolean success = actionChain.action(activityEntity, activitySkuEntity, activityCountEntity);

        // 4. 构建订单聚合对象
        CreateQuotaOrderAggregate createQuotaOrderAggregate = buildOrderAggregate(skuRechargeEntity, activityEntity, activitySkuEntity, activityCountEntity);

        // 5. 根据不同的订单类型，选择不同的策略
        ITradePolicy tradePolicy = tradePolicyGroup.get(skuRechargeEntity.getTradeType().getCode());
        tradePolicy.trade(createQuotaOrderAggregate);

        // 6. 返回单号
        return UnpaidActivityOrderEntity.builder()
                .userId(userId)
                .orderId(createQuotaOrderAggregate.getActivityOrderEntity().getOrderId())
                .payAmount(createQuotaOrderAggregate.getActivityOrderEntity().getPayAmount())
                .outBusinessNo(outBusinessNo)
                .build();
    }

    protected abstract CreateQuotaOrderAggregate buildOrderAggregate(SkuRechargeEntity skuRechargeEntity, ActivityEntity activityEntity, ActivitySkuEntity activitySkuEntity, ActivityCountEntity activityCountEntity);
}
