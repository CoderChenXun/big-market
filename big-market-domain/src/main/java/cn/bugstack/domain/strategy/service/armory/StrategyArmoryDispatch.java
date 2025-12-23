package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

/**
 * 实现抽奖的调度和装配
 */
@Slf4j
@Service
public class StrategyArmoryDispatch implements IStrategyArmory, IStrategyDispatch {

    @Resource
    private IStrategyRepository repository;

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
        boolean allStrategyAssemble = assembleLotteryStrategy(String.valueOf(strategyId), strategyAwardList);
        if (!allStrategyAssemble) {
            log.error("策略ID：{}，配置全量策略失败", strategyId);
            return false;
        }

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
            assembleLotteryStrategy(String.valueOf(strategyId).concat("_").concat(key), strategyAwardEntitiesClone);
        }
        return true;
    }

    @Override
    public boolean strategyArmoryByActivityId(Long activityId) {
        // 首先查询策略ID，然后根据策略ID进行装配
        Long strategyId = repository.queryStrategyIdByActivityId(activityId);
        return assembleLotteryStrategy(strategyId);
    }

    private void cacheStrategyAwardCount(Long strategyId, Integer awardId, Integer awardCount) {
        log.info("缓存策略ID：{}，奖品ID：{}，奖品数量：{}", strategyId, awardId, awardCount);
        repository.cacheStrategyAwardCount(strategyId, awardId, awardCount);
    }

    public boolean assembleLotteryStrategy(String key, List<StrategyAwardEntity> strategyAwardList) {
        // 1. 获取最小概率值
        BigDecimal minAwardRate = strategyAwardList.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        // 2. 获取概率总值
        BigDecimal totalAwardRate = strategyAwardList.stream()
                .map(StrategyAwardEntity::getAwardRate)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 3. 计算rateRange
        BigDecimal rateRange = totalAwardRate.divide(minAwardRate, 0, RoundingMode.CEILING);

        // 4. 生成策略奖品概率查找表
        List<Integer> strategyAwardSearchRateTables = new ArrayList<>(rateRange.intValue());
        for (StrategyAwardEntity strategyAward : strategyAwardList) {
            // 4.1 奖品概率
            Integer awardId = strategyAward.getAwardId();
            BigDecimal awardRate = strategyAward.getAwardRate();
            // 4.2 奖品概率区间
            for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
                strategyAwardSearchRateTables.add(awardId);
            }
        }

        // 5. 对存储的奖品进行乱序操作
        Collections.shuffle(strategyAwardSearchRateTables);

        // 6.生成Map集合
        Map<Integer, Integer> shuffleStrategyAwardSearchRateTables = new LinkedHashMap<>();
        for (int i = 0; i < strategyAwardSearchRateTables.size(); i++) {
            shuffleStrategyAwardSearchRateTables.put(i, strategyAwardSearchRateTables.get(i));
        }

        // 7. 保存strategyId、rateRange和shuffleStrategyAwardSearchRateTables到redis中
        repository.storeStrategyAwardSearchRateTables(key, shuffleStrategyAwardSearchRateTables.size(), shuffleStrategyAwardSearchRateTables);
        return true;
    }

    @Override
    public Integer getRandomAwardId(Long strategyId) {
        String key = String.valueOf(strategyId);
        // 1. 获取策略奖品概率查找表大小
        int rateRange = repository.getRateRange(key);
        // 2. 通过rateRange生成随机数，获取奖品ID
        return repository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
    }

    @Override
    public Integer getRandomAwardId(Long strategyId, String ruleWeightValue) {
        String cachedKey = String.valueOf(strategyId).concat("_").concat(ruleWeightValue);
        // 1. 通过策略ID和规则权重值获取策略奖品概率查找表大小
        int rateRange = repository.getRateRange(cachedKey);
        // 2. 通过rateRange生成随机数，获取奖品ID
        return repository.getStrategyAwardAssemble(cachedKey, new SecureRandom().nextInt(rateRange));
    }

    @Override
    public Boolean subtractionAwardStock(Long strategyId, Integer awardId) {
        String cachedKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY + strategyId + Constants.UNDERLINE + awardId;
        return repository.subtractionAwardStock(cachedKey);
    }
}
