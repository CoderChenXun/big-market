package cn.bugstack.domain.strategy.service.raffle;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleTreeVO;
import cn.bugstack.domain.strategy.model.valobj.RuleWeightVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.AbstractRaffleStrategy;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleRule;
import cn.bugstack.domain.strategy.service.IRaffleStock;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.DefaultTreeFactory;
import cn.bugstack.domain.strategy.service.rule.tree.factory.engine.IDecisionTreeEngine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class DefaultRaffleStrategy extends AbstractRaffleStrategy implements IRaffleStock, IRaffleAward, IRaffleRule {

    public DefaultRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory, DefaultTreeFactory defaultTreeFactory) {
        super(repository, strategyDispatch, defaultChainFactory, defaultTreeFactory);
    }

    @Override
    public DefaultChainFactory.StrategyAwardVO raffleLogicChain(String userId, Long strategyId) {
        // 1. 通过策略ID获取抽奖逻辑链
        ILogicChain loginChain = defaultChainFactory.openLogicChain(strategyId);
        // 2. 开启逻辑链
        DefaultChainFactory.StrategyAwardVO chainStrategyAwardVO = loginChain.logic(userId, strategyId);
        return chainStrategyAwardVO;
    }

    @Override
    public DefaultTreeFactory.StrategyAwardVO raffleLogicTree(String userId, Long strategyId, Integer awardId, Date endDateTime) {
        // 1. 查询treeId
        StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModels(strategyId, awardId);
        if (null == strategyAwardRuleModelVO) {
            return DefaultTreeFactory.StrategyAwardVO.builder().awardId(awardId).build();
        }
        // 2. 查询数据库，组装出ruleTreeVo
        RuleTreeVO ruleTreeVO = repository.queryRuleTreeVo(strategyAwardRuleModelVO.getRuleModels());
        if (null == ruleTreeVO) {
            throw new RuntimeException("存在抽奖策略配置的规则模型 Key，未在库表 rule_tree、rule_tree_node、rule_tree_line 配置对应的规则树信息 " + strategyAwardRuleModelVO.getRuleModels());
        }
        IDecisionTreeEngine decisionTreeEngine = defaultTreeFactory.openLogicTree(ruleTreeVO);

        // 3. 决策树进行处理
        DefaultTreeFactory.StrategyAwardVO treeStrategyAwardVO = decisionTreeEngine.process(userId, strategyId, awardId, endDateTime);
        return treeStrategyAwardVO;
    }

    @Override
    public StrategyAwardStockKeyVO takeQueueValue() throws InterruptedException {
        // 从阻塞队列中取出需要扣减库存的策略ID和奖品ID -》 StrategyAwardStockKeyVO
        return repository.takeQueueValue();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        // 更新数据库的奖品库存
        repository.updateStrategyAwardStock(strategyId, awardId);
    }

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardListByStrategyId(Long strategyId) {
        List<StrategyAwardEntity> awardEntityList = repository.queryStrategyAwardListByStrategyId(strategyId);
        return awardEntityList;
    }

    @Override
    public List<StrategyAwardEntity> queryStrategyAwardListByActivityId(Long activityId) {
        List<StrategyAwardEntity> awardEntityList = repository.queryStrategyAwardListByActivityId(activityId);
        return awardEntityList;
    }

    @Override
    public Map<String, Integer> queryAwardRuleLockCount(String[] treeIds) {
        // 根据treeId查询对应根决策节点rule_lock对应的解锁值
        Map<String, Integer> ruleLockCount = repository.queryAwardRuleLockCount(treeIds);
        return ruleLockCount;
    }

    @Override
    public List<RuleWeightVO> queryRuleWeightByActivityId(Long activityId) {
        // 1. 首先根据activityId查询出strategyId
        Long strategyId = repository.queryStrategyIdByActivityId(activityId);
        return queryRuleWeightByStrategyId(strategyId);
    }

    @Override
    public List<RuleWeightVO> queryRuleWeightByStrategyId(Long StrategyId) {
        // 根据策略Id 查询出规则权重
        return repository.queryRuleWeightByStrategyId(StrategyId);
    }
}
