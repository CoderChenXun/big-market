package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2026-01-09 15:17
 **/
@Slf4j
public abstract class AbstractStrategyAlgorithm implements IStrategyArmory,IStrategyDispatch{

    @Resource
    protected IStrategyRepository repository;

    /**
     * 装配入口
     * @param activityId 活动ID
     * @return 配置结果
     */
    @Override
    public boolean strategyArmoryByActivityId(Long activityId) {
        // 首先查询策略ID，然后根据策略ID进行装配
        Long strategyId = repository.queryStrategyIdByActivityId(activityId);
        return assembleLotteryStrategy(strategyId);
    }

    /**
     * 装配抽奖策略
     *
     * @param strategyId
     * @return
     */
    @Override
    public boolean assembleLotteryStrategy(Long strategyId) {
        // 1. 查询策略配置
        List<StrategyAwardEntity> strategyAwardList = repository.queryStrategyAwardListByStrategyId(strategyId);
        // 1.1 查询到的奖品为null或者数量为0
        if (null == strategyAwardList || strategyAwardList.isEmpty()) {
            log.error("策略ID：{}，未配置抽奖奖品", strategyId);
            return false;
        }

        // 1.2 缓存奖品库存
        for (StrategyAwardEntity strategyAward : strategyAwardList) {
            Integer awardId = strategyAward.getAwardId();
            Integer awardCount = strategyAward.getAwardCount();
            cacheStrategyAwardCount(strategyId,awardId, awardCount);
        }

        // 2. 装配全量策略
        armoryAlgorithm(String.valueOf(strategyId), strategyAwardList);

        // 3. 权重策略配置 -- 配置Rule_Weight策略
        StrategyEntity strategyEntity = repository.queryStrategyEntityByStrategyId(strategyId);
        if (strategyEntity == null) {
            return true;
        }
        // 3.1 查询是否有rule_weight规则
        String ruleWeight = strategyEntity.getRuleWeight();
        // 3.2 配置rule_weight规则为空
        if (ruleWeight == null) {
            return true;
        }
        StrategyRuleEntity strategyRuleEntity = repository.queryStrategyRule(strategyId, ruleWeight);
        if (strategyRuleEntity == null) {
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(), ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getInfo());
        }
        // 3.3 配置rule_weight规则不为空,解析rule_weight规则
        Map<String, List<Integer>> ruleWeightMap = strategyRuleEntity.getRuleWeightValues();
        if (ruleWeightMap == null) {
            throw new AppException(ResponseCode.STRATEGY_RULE_WEIGHT_IS_NULL.getCode(), "解析rule_weight规则失败");
        }
        Set<String> keySet = ruleWeightMap.keySet();
        for (String key : keySet) {
            List<Integer> value = ruleWeightMap.get(key);
            if (value == null || value.isEmpty()) {
                continue;
            }
            // 3.4 配置rule_weight规则不为空,生成策略奖品概率查找表
            // 3.5 基于全量策略奖品概率查找表生成策略奖品概率查找表
            List<StrategyAwardEntity> strategyAwardEntitiesClone = new ArrayList<>(strategyAwardList);
            strategyAwardEntitiesClone.removeIf(entity -> !value.contains(entity.getAwardId()));
            // 3.6 将克隆的策略奖品概率查找表进行装配
            // rule_weight 规则奖品配置
            armoryAlgorithm(String.valueOf(strategyId).concat("_").concat(key), strategyAwardEntitiesClone);
        }
        return true;
    }

    protected abstract void armoryAlgorithm(String valueOf, List<StrategyAwardEntity> strategyAwardList);

    private void cacheStrategyAwardCount(Long strategyId, Integer awardId, Integer awardCount) {
        log.info("缓存策略ID：{}，奖品ID：{}，奖品数量：{}", strategyId, awardId, awardCount);
        repository.cacheStrategyAwardCount(strategyId, awardId, awardCount);
    }
    

    protected BigDecimal minAwardRate(List<StrategyAwardEntity> strategyAwardList) {
        // 1. 获取最小概率值
        BigDecimal minAwardRate = strategyAwardList.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
        return minAwardRate;
    }

    protected double convert(double min) {
        if (0 == min) return 1D;

        String minStr = String.valueOf(min);

        // 小数点前
        String beginVale = minStr.substring(0, minStr.indexOf("."));
        int beginLength = 0;
        if (Double.parseDouble(beginVale) > 0) {
            beginLength = minStr.substring(0, minStr.indexOf(".")).length();
        }

        // 小数点后
        String endValue = minStr.substring(minStr.indexOf(".") + 1);
        int endLength = 0;
        if (Double.parseDouble(endValue) > 0) {
            endLength = minStr.substring(minStr.indexOf(".") + 1).length();
        }

        return Math.pow(10, beginLength + endLength);
    }
    
    // 默认抽奖
    @Override
    public Integer getRandomAwardId(Long strategyId) {
        String key = String.valueOf(strategyId);
        return dispatchAlgorithm(key);
    }
    
    // 权重抽奖
    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        String cachedKey = String.valueOf(strategyId).concat(Constants.UNDERLINE).concat(ruleWeightValue);
        return dispatchAlgorithm(cachedKey);
    }

    protected abstract Integer dispatchAlgorithm(String cachedKey);

    @Override
    public Boolean subtractionAwardStock(Long strategyId, Integer awardId,Date endDateTime) {
        String cachedKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY + strategyId + Constants.UNDERLINE + awardId;
        return repository.subtractionAwardStock(cachedKey,endDateTime);
    }
}
