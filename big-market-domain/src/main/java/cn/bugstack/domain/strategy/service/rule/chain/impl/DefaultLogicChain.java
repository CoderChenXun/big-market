package cn.bugstack.domain.strategy.service.rule.chain.impl;

import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.AbstractLogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component("default")
public class DefaultLogicChain extends AbstractLogicChain {
    private IStrategyDispatch strategyDispatch;

    public DefaultLogicChain(IStrategyDispatch strategyDispatch) {
        this.strategyDispatch = strategyDispatch;
    }

    @Override
    public Integer logic(String userId, Long strategyId) {
        log.info("抽奖责任链-默认开始 userId: {} strategyId: {} ruleModel: {}", userId, strategyId, ruleModel());
        Integer awardId = strategyDispatch.getRandomAwardId(strategyId);
        return awardId;
    }


    @Override
    protected String ruleModel() {
        return "default";
    }
}
