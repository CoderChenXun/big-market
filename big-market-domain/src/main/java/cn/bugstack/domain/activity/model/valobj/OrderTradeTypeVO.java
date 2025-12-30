package cn.bugstack.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 19:43
 **/
@Getter
@AllArgsConstructor
public enum OrderTradeTypeVO {
    credit_pay_trade("credit_pay_trade", "积分支付交易"),
    rebate_no_pay_trade("rebate_no_pay_trade", "返利不需要支付交易");

    private final String code;
    private final String desc;
}
