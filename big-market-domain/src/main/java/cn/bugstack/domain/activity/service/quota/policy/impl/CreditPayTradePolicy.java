package cn.bugstack.domain.activity.service.quota.policy.impl;

import cn.bugstack.domain.activity.model.aggregate.CreateQuotaOrderAggregate;
import cn.bugstack.domain.activity.model.valobj.OrderStateVO;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.quota.policy.ITradePolicy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @Author: coderLan
 * @Description: 积分支付策略
 * @DateTime: 2025-12-29 19:16
 **/
@Slf4j
@Component("credit_pay_trade")
public class CreditPayTradePolicy implements ITradePolicy {

    private IActivityRepository repository;

    public CreditPayTradePolicy(IActivityRepository repository) {
        this.repository = repository;
    }

    @Override
    public void trade(CreateQuotaOrderAggregate createQuotaOrderAggregate) {
        createQuotaOrderAggregate.setOrderState(OrderStateVO.wait_pay);
        repository.doSaveCreditPayOrder(createQuotaOrderAggregate);
    }
}
