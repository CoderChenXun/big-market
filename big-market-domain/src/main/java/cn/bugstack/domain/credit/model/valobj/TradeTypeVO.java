package cn.bugstack.domain.credit.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 15:57
 **/
@Getter
@AllArgsConstructor
public enum TradeTypeVO {

    FORWARD("forward", "正向交易，+积分"),
    REVERSE("reverse", "反向交易，-积分");

    private final String code;
    private final String desc;
}
