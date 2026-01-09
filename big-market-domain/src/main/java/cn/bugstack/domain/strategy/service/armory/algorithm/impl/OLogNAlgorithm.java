package cn.bugstack.domain.strategy.service.armory.algorithm.impl;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.armory.algorithm.AbstractAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * @Author: coderLan
 * @Description: O(logN)
 * @DateTime: 2026-01-09 13:16
 **/
@Slf4j
@Component("oLogNAlgorithm")
public class OLogNAlgorithm  extends AbstractAlgorithm {

    @Resource
    private ThreadPoolExecutor executor;

    @Override
    public boolean armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardList, BigDecimal rateRange) {
        // LonN算法策略的全量装配或rule_weight装配
        log.info("抽奖算法 O(logN) 策略装配 key:{}", key);
        int from = 1;
        int to = 0;
        Map<Map<Integer, Integer>,Integer> tables = new HashMap<>();
        for (StrategyAwardEntity strategyAward : strategyAwardList) {
            // 根据rateRange和奖品概率计算奖品区间
            to = to + rateRange.multiply(strategyAward.getAwardRate()).intValue();
            Map<Integer, Integer> newMap = new HashMap<>();
            newMap.put(from, to);
            tables.put(newMap, strategyAward.getAwardId());
            // 计算下一个奖品抽奖区间的开始区间
            from = to + 1;
        }

        repository.storeStrategyAwardSearchRateTables(key, to, tables);
        return true;
    }

    @Override
    public Integer dispatchAlgorithm(String key) {
        // 基于策略Id进行抽奖
        log.info("抽奖算法 O(LogN) 进行抽奖 key:{}", key);
        // 全量策略的Key就是策略ID，rule_weight策略的Key就是 strategyId_ruleWeight
        // 1. 获取策略奖品概率查找表大小 和 策略奖品概率查找表
        int rateRange = repository.getRateRange(key);
        Map<Map<String, Integer>,Integer> tables = repository.getMap(key);
        if(tables.size() <= 8){
            log.info("抽奖算法 O(N) 抽奖计算（循环） key:{}", key);
            return forSearch(secureRandom.nextInt(rateRange), tables);
        }else if(tables.size() <= 16){
            log.info("抽奖算法 O(LogN) 抽奖计算（二分） key:{}", key);
            return binarySearch(secureRandom.nextInt(rateRange), tables);
        }else {
            log.info("抽奖算法 O(LogN) 抽奖计算（多线程） key:{}", key);
            return threadSearch(secureRandom.nextInt(rateRange), tables);
        }
    }

    private Integer forSearch(int rateKey, Map<Map<String, Integer>, Integer> tables) {
        Integer awardId = null;
        // 1. 循环查找
        for(Map.Entry<Map<String, Integer>, Integer> entry : tables.entrySet()){
            Map<String, Integer> key = entry.getKey();

            for(Map.Entry<String, Integer> keyEntry : key.entrySet()){
                int from = Integer.parseInt(keyEntry.getKey());
                int to = keyEntry.getValue();
                if(rateKey >= from && rateKey <= to){
                    awardId = entry.getValue();
                    break;
                }
            }

            if(awardId != null){
                break;
            }
        }

        return awardId;
    }

    private Integer binarySearch(int rateKey, Map<Map<String, Integer>, Integer> tables) {
        List<Map.Entry<Map<String, Integer>, Integer>> entries = new ArrayList<>(tables.entrySet());
        entries.sort(Comparator.comparingInt(e -> Integer.parseInt(e.getKey().keySet().iterator().next())));

        int left = 0;
        int right = entries.size() - 1;

        while (left <= right) {
            int middle = left + (right - left) >> 1;
            Map.Entry<Map<String, Integer>, Integer> entry = entries.get(middle);
            Map<String, Integer> rangeMap = entry.getKey();
            Map.Entry<String, Integer> range = rangeMap.entrySet().iterator().next();
            int from = Integer.parseInt(range.getKey());
            int to = range.getValue();

            if (rateKey >= from && rateKey <= to) {
                return entry.getValue();
            }else if (rateKey < from) {
                right = middle - 1;
            } else {
                left = middle + 1;
            }
        }
        return null;
    }

    private Integer threadSearch(int rateKey, Map<Map<String, Integer>, Integer> tables) {
        // 多线程抽奖
        List<CompletableFuture<Map.Entry<Map<String, Integer>, Integer>>> futures = tables.entrySet().stream()
                .map(entry -> CompletableFuture.supplyAsync(() -> {
                    Map<String, Integer> rangeMap = entry.getKey();
                    for (Map.Entry<String, Integer> rangeEntry : rangeMap.entrySet()) {
                        int start = Integer.parseInt(rangeEntry.getKey());
                        int end = rangeEntry.getValue();
                        if (rateKey >= start && rateKey <= end) {
                            return entry;
                        }
                    }
                    return null;
                }, executor))
                .collect(Collectors.toList());

        CompletableFuture<Void> allFutures = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

        try {
            // 等待所有异步任务完成，同时返回第一个匹配的结果
            allFutures.join();
            for (CompletableFuture<Map.Entry<Map<String, Integer>, Integer>> future : futures) {
                Map.Entry<Map<String, Integer>, Integer> result = future.getNow(null);
                if (result != null) {
                    return result.getValue();
                }
            }
        } catch (CompletionException e) {
            e.printStackTrace();
        }

        return null;
    }
}
