package cn.bugstack.domain.strategy.service.rule.chain;

public interface ILogicChainArmory {
    ILogicChain appendNext(ILogicChain logicChain);

    ILogicChain next();
}
