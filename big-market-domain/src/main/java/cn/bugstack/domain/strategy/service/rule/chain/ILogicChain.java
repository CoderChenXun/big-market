package cn.bugstack.domain.strategy.service.rule.chain;

/**
 * 抽奖前的策略规则责任链接口
 * 输入参数：用户ID，策略ID
 * 输出参数：抽奖结果（奖品ID）
 */
public interface ILogicChain extends ILogicChainArmory {
    Integer logic(String userId, Long strategyId);
}
