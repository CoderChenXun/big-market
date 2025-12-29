package cn.bugstack.domain.credit.repository;

import cn.bugstack.domain.credit.model.aggregate.TradeAggregate;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 16:02
 **/
public interface ICreditRepository {
    void saveUserCreditTradeOrder(TradeAggregate tradeAggregate);
}
