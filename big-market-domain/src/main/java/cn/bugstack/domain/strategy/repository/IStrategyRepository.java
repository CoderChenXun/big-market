package cn.bugstack.domain.strategy.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;

import java.util.List;
import java.util.Map;

public interface IStrategyRepository {
    List<StrategyAwardEntity> queryStrategyAwardListByStrategyId(Long strategyId);

    void storeStrategyAwardSearchRateTables(String key, Integer tableSize, Map<Integer, Integer> shuffleStrategyAwardSearchRateTables);

    int getRateRange(String key);

    Integer getStrategyAwardAssemble(String key, int nextInt);

    StrategyEntity queryStrategyEntityByStrategyId(Long strategyId);

    StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel);

    String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel);

    String queryStrategyRuleValue(Long strategyId, String ruleModel);

    StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, Integer awardId);
}
