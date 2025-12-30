package cn.bugstack.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum OrderStateVO {

    wait_pay("wait_pay", "待支付"),
    completed("completed", "完成");

    private final String code;
    private final String desc;

}
