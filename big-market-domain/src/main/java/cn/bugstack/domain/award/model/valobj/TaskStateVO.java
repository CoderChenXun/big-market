package cn.bugstack.domain.award.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TaskStateVO {
    create("create", "创建"),
    complete("complete", "完成"),
    fail("fail", "失败"),
    ;

    private final String code;
    private final String desc;
}
