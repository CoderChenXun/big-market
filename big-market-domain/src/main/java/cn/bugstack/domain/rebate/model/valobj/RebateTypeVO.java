package cn.bugstack.domain.rebate.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum RebateTypeVO {
    SKU("sku", "活动库存充值商品"),
    INTEGRAL("integral", "积分充值商品");

    private final String code;
    private final String info;
}
