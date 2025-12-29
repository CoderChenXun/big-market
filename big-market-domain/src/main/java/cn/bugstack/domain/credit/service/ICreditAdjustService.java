package cn.bugstack.domain.credit.service;

import cn.bugstack.domain.credit.model.entity.TradeEntity;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 15:51
 **/
public interface ICreditAdjustService {
    /**
     * 创建增加积分额度订单
     *
     * @param tradeEntity 交易实体
     * @return 单号
     */
    String createOrder(TradeEntity tradeEntity);
}
