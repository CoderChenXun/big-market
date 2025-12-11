package cn.bugstack.domain.strategy.service.rule.filter.impl;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.entity.RuleMatterEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.filter.ILogicFilter;
import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
@LogicStrategy(logicMode = DefaultLogicFactory.LogicModel.RULE_LOCK)
public class RuleLockLogicFilter implements ILogicFilter<RuleActionEntity.RaffleCenterEntity> {

    // 抽奖次数
    public Long userRaffleCount = 0L;
    @Resource
    private IStrategyRepository repository;

    /**
     * 抽奖规则过滤
     *
     * @param ruleMatterEntity
     * @return
     */
    @Override
    public RuleActionEntity<RuleActionEntity.RaffleCenterEntity> filter(RuleMatterEntity ruleMatterEntity) {
        log.info("规则过滤-lock范围 userId:{} strategyId:{} ruleModel:{}", ruleMatterEntity.getUserId(), ruleMatterEntity.getStrategyId(), ruleMatterEntity.getRuleModel());
        String userId = ruleMatterEntity.getUserId();
        Long strategyId = ruleMatterEntity.getStrategyId();
        Integer awardId = ruleMatterEntity.getAwardId();

        // 查询当前策略奖品rule_lock规则需要的解锁次数
        String ruleValue = repository.queryStrategyRuleValue(strategyId, awardId, ruleMatterEntity.getRuleModel());

        // 将当前解锁次数与当前用户解锁次数进行对比
        if (ruleValue == null) {
            return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                    .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                    .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                    .build();
        }

        Long awardUnlockCount = Long.parseLong(ruleValue);
        if (awardUnlockCount > userRaffleCount) {
            // 用户抽奖次数小于当前解锁次数
            return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                    .code(RuleLogicCheckTypeVO.TAKE_OVER.getCode())
                    .info(RuleLogicCheckTypeVO.TAKE_OVER.getInfo())
                    .build();
        }

        return RuleActionEntity.<RuleActionEntity.RaffleCenterEntity>builder()
                .code(RuleLogicCheckTypeVO.ALLOW.getCode())
                .info(RuleLogicCheckTypeVO.ALLOW.getInfo())
                .build();
    }

}
