package cn.bugstack.domain.strategy.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

import java.util.Date;
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

    RuleTreeVO queryRuleTreeVo(String treeId);

    void cacheStrategyAwardCount(Long strategyId, Integer awardId, Integer awardCount);

    Boolean subtractionAwardStock(String cachedKey, Date endDateTime);
    Boolean subtractionAwardStock(String cachedKey);

    void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO);

    StrategyAwardStockKeyVO takeQueueValue();

    void updateStrategyAwardStock(Long strategyId, Integer awardId);

    StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId);

    Long queryStrategyIdByActivityId(Long activityId);

    List<StrategyAwardEntity> queryStrategyAwardListByActivityId(Long activityId);

    Map<String, Integer> queryAwardRuleLockCount(String[] treeIds);
}
