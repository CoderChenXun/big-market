package cn.bugstack.domain.credit.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 15:55
 **/
@Getter
@AllArgsConstructor
public enum TradeNameVO {

    REBATE("行为返利"),
    CONVERT_SKU("兑换抽奖");

    private final String name;
}
