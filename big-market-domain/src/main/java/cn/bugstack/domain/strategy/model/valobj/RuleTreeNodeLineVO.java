package cn.bugstack.domain.strategy.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleTreeNodeLineVO {
    /**
     * 规则树ID
     */
    private String treeId;

    /**
     * 规则key节点from
     */
    private String ruleNodeFrom;

    /**
     * 规则key节点to
     */
    private String ruleNodeTo;

    /**
     * 限定类型；1:=;2:>;3:<;4:>=;5<=;6:enum[枚举范围]
     */
    private RuleLimitTypeVO ruleLimitType;

    /**
     * 限定值（到下个节点）
     */
    private RuleLogicCheckTypeVO ruleLimitValue;

}
