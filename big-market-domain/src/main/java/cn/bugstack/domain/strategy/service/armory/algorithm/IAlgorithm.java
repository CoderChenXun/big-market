package cn.bugstack.domain.strategy.service.armory.algorithm;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;

import java.math.BigDecimal;
import java.util.List;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2026-01-09 11:14
 **/
public interface IAlgorithm {
    // 对算法策略进行装配，用于后续的抽奖
    boolean armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardList, BigDecimal rateRange);

    // 抽奖,基于策略ID进行抽奖
    Integer dispatchAlgorithm(String key);
}
