package cn.bugstack.domain.strategy.service.armory.algorithm.impl;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.armory.algorithm.AbstractAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.*;

/**
 * @Author: coderLan
 * @Description: 抽奖算法-O1
 * @DateTime: 2026-01-09 11:25
 **/
@Slf4j
@Component("o1Algorithm")
public class O1Algorithm extends AbstractAlgorithm {

    /**
     * 装配策略
     * @param key 用户redis后续拼接作为缓冲Key
     * @param strategyAwardList 当前需要缓存的策略奖品列表
     * @param rateRange 策略奖品概率区间
     */
    @Override
    public boolean armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardList, BigDecimal rateRange) {
        log.info("抽奖算法 O(1) 策略装配 key:{}", key);
        // 1. 生成策略奖品概率查找表
        List<Integer> strategyAwardSearchRateTables = new ArrayList<>(rateRange.intValue());
        for (StrategyAwardEntity strategyAward : strategyAwardList) {
            // 1.1 奖品概率
            Integer awardId = strategyAward.getAwardId();
            BigDecimal awardRate = strategyAward.getAwardRate();
            // 1.2 奖品概率区间
            for (int i = 0; i < rateRange.multiply(awardRate).setScale(0, RoundingMode.CEILING).intValue(); i++) {
                strategyAwardSearchRateTables.add(awardId);
            }
        }

        // 2. 对存储的奖品进行乱序操作
        Collections.shuffle(strategyAwardSearchRateTables);

        // 3.生成Map集合
        Map<Integer, Integer> shuffleStrategyAwardSearchRateTables = new LinkedHashMap<>();
        for (int i = 0; i < strategyAwardSearchRateTables.size(); i++) {
            shuffleStrategyAwardSearchRateTables.put(i, strategyAwardSearchRateTables.get(i));
        }

        // 4. 保存strategyId、rateRange和shuffleStrategyAwardSearchRateTables到redis中
        repository.storeStrategyAwardSearchRateTables(key, shuffleStrategyAwardSearchRateTables.size(), shuffleStrategyAwardSearchRateTables);
        return true;
    }

    @Override
    public Integer dispatchAlgorithm(String key) {
        log.info("抽奖算法 O(1) 进行抽奖 key:{}", key);
        // 全量策略的Key就是策略ID，rule_weight策略的Key就是 strategyId_ruleWeight
        // 1. 获取策略奖品概率查找表大小
        int rateRange = repository.getRateRange(key);
        // 2. 通过rateRange生成随机数，获取奖品ID
        return repository.getStrategyAwardAssemble(key, new SecureRandom().nextInt(rateRange));
    }
}
