package cn.bugstack.domain.strategy.service.rule.chain;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractLogicChain implements ILogicChain {
    private ILogicChain next;

    @Override
    public ILogicChain appendNext(ILogicChain logicChain) {
        this.next = logicChain;
        return logicChain;
    }

    @Override
    public ILogicChain next() {
        return next;
    }

    protected abstract String ruleModel();
}
