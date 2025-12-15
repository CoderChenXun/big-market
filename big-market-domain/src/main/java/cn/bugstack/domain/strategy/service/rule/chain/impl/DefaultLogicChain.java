package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("rule_default")
public class DefaultLogicChain extends AbstractLogicChain {
    private IStrategyDispatch strategyDispatch;

    public DefaultLogicChain(IStrategyDispatch strategyDispatch) {
        this.strategyDispatch = strategyDispatch;
    }

    @Override
    public DefaultChainFactory.StrategyAwardVO logic(String userId, Long strategyId) {
        log.info("抽奖责任链-默认开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId);
        return DefaultChainFactory.StrategyAwardVO.builder()
                .awardId(awardId)
                .logicModel(ruleModel())
                .build();
    }


    @Override
    protected String ruleModel() {
        return "rule_default";
    }
}
