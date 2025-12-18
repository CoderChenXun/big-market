package cn.bugstack.domain.activity.service.rule;

public interface IActionChainArmory {

    IActionChain appendNext(IActionChain next);

    IActionChain next();
}
