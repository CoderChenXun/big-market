package cn.bugstack.domain.strategy.service.raffle;

import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.model.valobj.RuleLogicCheckTypeVO;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
import cn.bugstack.domain.strategy.service.rule.chain.ILogicChain;
import cn.bugstack.domain.strategy.service.rule.chain.factory.DefaultChainFactory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Slf4j
public abstract class AbstractRaffleStrategy implements IRaffleStrategy {

    // 策略仓储服务 -> domain层像一个大厨，仓储层提供米面粮油
    protected IStrategyRepository repository;

    // 策略调度服务 -> 只负责抽奖处理，通过新增接口的方式，隔离职责，不需要使用方关心或者调用抽奖的初始化
    protected IStrategyDispatch strategyDispatch;

    private final DefaultChainFactory defaultChainFactory;

    public AbstractRaffleStrategy(IStrategyRepository repository, IStrategyDispatch strategyDispatch, DefaultChainFactory defaultChainFactory) {
        this.repository = repository;
        this.strategyDispatch = strategyDispatch;
        this.defaultChainFactory = defaultChainFactory;
    }

    @Override
    public RaffleAwardEntity performRaffle(RaffleFactorEntity raffleFactorEntity) {
        // 1. 参数校验
        String userId = raffleFactorEntity.getUserId();
        Long strategyId = raffleFactorEntity.getStrategyId();
        if (null == strategyId || StringUtils.isBlank(userId)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 2. 获取抽奖责任链
        ILogicChain logicChain = defaultChainFactory.openLogicChain(strategyId);
        // 3. 进行责任链处理
        Integer awardId = logicChain.logic(userId, strategyId);
        // 3.1 查询当前的抽到的AwardI对应的rule_models信息
        StrategyAwardRuleModelVO strategyAwardRuleModelVO = repository.queryStrategyAwardRuleModels(strategyId, awardId);
        // 3.2 对当前的rule_models进行处理
        String[] ruleModelArr = strategyAwardRuleModelVO.raffleCenterRuleModelList();
        // 3.3 抽奖中规则过滤
        RuleActionEntity<RuleActionEntity.RaffleCenterEntity> raffleCenterActionEntity = this.doCheckRaffleCenterLogic(RaffleFactorEntity.builder().strategyId(strategyId).awardId(awardId).userId(userId).build(), ruleModelArr);

        // 3.4 如果进行规则过滤后需要接管
        if (RuleLogicCheckTypeVO.TAKE_OVER.getCode().equals(raffleCenterActionEntity.getCode())) {
            log.info("【临时日志】中奖中规则拦截，通过抽奖后规则 rule_luck_award 走兜底奖励。");
            return RaffleAwardEntity.builder()
                    .awardDesc("中奖中规则拦截，通过抽奖后规则 rule_luck_award 走兜底奖励。")
                    .build();
        }

        // 4. 抽奖中规则过滤，当前抽奖抽中的奖品，有可能需要达到特定的抽奖规则才能抽中，此时需要进行过滤
        return RaffleAwardEntity.builder()
                .awardId(awardId)
                .build();
    }

    protected abstract RuleActionEntity<RuleActionEntity.RaffleBeforeEntity> doCheckRaffleBeforeLogic(RaffleFactorEntity raffleFactorEntity, String... logics);

    protected abstract RuleActionEntity<RuleActionEntity.RaffleCenterEntity> doCheckRaffleCenterLogic(RaffleFactorEntity raffleFactorEntity, String... logics);
}
