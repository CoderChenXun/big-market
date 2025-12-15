package cn.bugstack.domain.strategy.service.rule.chain.factory;

import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import lombok.*;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class DefaultChainFactory {

    private Map<String, ILogicChain> logicChainMap;

    private IStrategyRepository repository;

    // 构造器注入
    public DefaultChainFactory(Map<String, ILogicChain> logicChainMap, IStrategyRepository repository) {
        this.logicChainMap = logicChainMap;
        this.repository = repository;
    }

    /**
     * 获取策略链
     *
     * @param strategyId 策略ID
     * @return 策略链
     */
    public ILogicChain openLogicChain(Long strategyId) {
        // 1. 查询strategy数据库进行策略查询
        StrategyEntity strategy = repository.queryStrategyEntityByStrategyId(strategyId);
        if (null == strategy) {
            return logicChainMap.get("rule_default");
        }
        // 2. 获取rule_models
        String[] ruleModels = strategy.ruleModels();
        if (null == ruleModels || ruleModels.length == 0) {
            return logicChainMap.get("rule_default");
        }

        // 按照配置顺序装填责任逻辑链，比如说：rule_blacklist -> rule_weight
        ILogicChain logicChain = logicChainMap.get(ruleModels[0]);
        ILogicChain current = logicChain;
        for (int i = 1; i < ruleModels.length; i++) {
            ILogicChain next = logicChainMap.get(ruleModels[i]);
            current = current.appendNext(next);
        }
        return logicChain;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class StrategyAwardVO{
        /**
         * 抽奖奖品ID - 抽奖结果
         */
        private Integer awardId;

        /**
         * 过滤操作获取奖品的规则
         */
        private String logicModel;
    }

    @Getter
    @AllArgsConstructor
    public enum LogicModel {

        RULE_DEFAULT("rule_default", "默认抽奖"),
        RULE_BLACKLIST("rule_blacklist", "黑名单抽奖"),
        RULE_WEIGHT("rule_weight", "权重规则"),
        ;

        private final String code;
        private final String info;

    }
}
