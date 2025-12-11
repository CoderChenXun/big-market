package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.types.common.Constants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component("rule_weight")
public class RuleWeightLogicChain extends AbstractLogicChain {
    private IStrategyRepository repository;

    private IStrategyDispatch strategyDispatch;

    private Long userRaffleCount = 4500L;

    public RuleWeightLogicChain(IStrategyRepository repository, IStrategyDispatch strategyDispatch) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
    }

    @Override
    public Integer logic(String userId, Long strategyId) {
        // 1. 查询value_weight规则在Strategy_rule表中对应的rule_value
        String ruleWeightValue = repository.queryStrategyRuleValue(strategyId, ruleModel());

        Map<Long, String> ruleWeightMap = getAnalyticalValue(ruleWeightValue);

        //  ruleWeightMap为：{1=[101, 102], 2=[103, 104]}
        ArrayList<Long> ruleWeightValueList = new ArrayList<>(ruleWeightMap.keySet());
        // 对ruleWeightValueSet进行排序
        Collections.sort(ruleWeightValueList);
        Long nextValue = ruleWeightValueList.stream()
                .filter(ruleValue -> userRaffleCount >= ruleValue)
                .findFirst()
                .orElse(null);
        // 如果不存在nextValue
        if (null == nextValue) {
            return next().logic(userId, strategyId);
        }
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId, ruleWeightMap.get(nextValue));
        log.info("抽奖责任链-权重接管 userId: {} strategyId: {} ruleModel: {} awardId: {}", userId, strategyId, ruleModel(), awardId);
        return awardId;
    }

    private Map<Long, String> getAnalyticalValue(String ruleWeightValue) {
        // 例如：4000:102,103,104,105 5000:102,103,104,105,106,107 6000:102,103,104,105,106,107,108,109
        String[] splitRuleWeightValue = ruleWeightValue.split(Constants.SPACE);
        Map<Long, String> ruleWeightMap = new HashMap<>();
        for (String ruleWeight : splitRuleWeightValue) {
            String[] splitRuleWeight = ruleWeight.split(Constants.COLON);
            if (splitRuleWeight == null || splitRuleWeight.length < 2) {
                log.error("抽奖责任链-权重规则错误 ruleModel: {} ruleWeight: {}", ruleModel(), ruleWeight);
                break;
            }
            ruleWeightMap.put(Long.valueOf(splitRuleWeight[0]), ruleWeight);
        }
        return ruleWeightMap;
    }

    @Override
    protected String ruleModel() {
        return "rule_weight";
    }
}
