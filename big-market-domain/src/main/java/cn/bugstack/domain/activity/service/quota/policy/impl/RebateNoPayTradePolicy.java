package cn.bugstack.domain.activity.service.quota.policy.impl;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.valobj.OrderStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description: 返利不需要支付
 * @DateTime: 2025-12-29 19:16
 **/
@Slf4j
@Component("rebate_no_pay_trade")
public class RebateNoPayTradePolicy implements ITradePolicy {

    private IActivityRepository repository;

    public RebateNoPayTradePolicy(IActivityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        // 返利订单直接设置为完成状态
        createQuotaOrderAggregate.setOrderState(OrderStateVO.completed);
        // 订单支付金额设置为0
        createQuotaOrderAggregate.getActivityOrderEntity().setPayAmount(BigDecimal.ZERO);
        repository.doSaveNoPayOrder(createQuotaOrderAggregate);
    }
}
