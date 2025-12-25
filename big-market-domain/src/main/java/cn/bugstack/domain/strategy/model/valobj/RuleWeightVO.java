package cn.bugstack.domain.strategy.model.valobj;

import lombok.*;

import java.util.List;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-25 15:03
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleWeightVO {
    // 原始规则值配置
    private String ruleValue;
    // 权重值
    private Integer weight;
    // 奖品配置
    private List<Integer> awardIds;
    // 奖品配置详情
    private List<Award> awardList;

    @Data
    public static class Award {
        private Integer awardId;
        private String awardTitle;
    }
}
