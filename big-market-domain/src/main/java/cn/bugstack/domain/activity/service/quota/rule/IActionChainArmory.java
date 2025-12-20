package cn.bugstack.domain.activity.service.quota.rule;

public interface IActionChainArmory {

    IActionChain appendNext(IActionChain next);

    IActionChain next();
}
