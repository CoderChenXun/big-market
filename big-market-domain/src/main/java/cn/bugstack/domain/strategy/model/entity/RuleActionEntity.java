package cn.bugstack.domain.strategy.model.entity;

import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RuleActionEntity<T extends RuleActionEntity.RaffleEntity> {
    private String code = RuleLogicCheckTypeVO.ALLOW.getCode();
    private String info = RuleLogicCheckTypeVO.ALLOW.getInfo();
    private T data;
    private String ruleModel;

    static public class RaffleEntity {
    }

    /**
     * 抽奖前
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    static public class RaffleBeforeEntity extends RaffleEntity {
        /**
         * 策略id
         */
        private Long strategyId;
        /**
         * 规则权重值
         */
        private String ruleWeightValueKey;
        /**
         * 奖品id
         */
        private Integer awardId;
    }

    /**
     * 抽奖中
     */
    /**
     * 抽奖前
     */
    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    @EqualsAndHashCode(callSuper = true)
    static public class RaffleCenterEntity extends RaffleEntity {
        /**
         * 策略id
         */
        private Long strategyId;
        /**
         * 规则权重值
         */
        private String ruleWeightValueKey;
        /**
         * 奖品id
         */
        private Integer awardId;
    }

    /**
     * 抽奖后
     */
    static public class RaffleAfterEntity extends RaffleEntity {

    }

}
