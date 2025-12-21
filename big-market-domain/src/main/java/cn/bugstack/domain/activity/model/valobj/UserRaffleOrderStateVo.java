package cn.bugstack.domain.activity.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum UserRaffleOrderStateVo {
    CREATE("create", "创建"),
    USED("used", "已使用"),
    CANCEL("cancel", "已作废");

    private final String code;
    private final String desc;
}
