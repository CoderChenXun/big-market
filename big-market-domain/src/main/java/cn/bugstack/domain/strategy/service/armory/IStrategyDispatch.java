package cn.bugstack.domain.strategy.service.armory;

import java.util.Date;

public interface IStrategyDispatch {
    // 获取奖品id
    Integer getRandomAwardId(Long strategyId);

    // 通过策略id和规则权重值进行抽奖
    Integer getRandomAwardId(Long strategyId, String ruleWeightValue);

    Boolean subtractionAwardStock(Long strategyId, Integer awardId, Date endDateTime);
}
