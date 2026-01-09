package cn.bugstack.domain.strategy.service.armory;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.armory.algorithm.AbstractAlgorithm;
import cn.bugstack.domain.strategy.service.armory.algorithm.IAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * 实现抽奖的调度和装配
 */
@Slf4j
@Service
public class StrategyArmoryDispatch extends AbstractStrategyAlgorithm {
    // 依赖注入算法策略
    private final Map<String, IAlgorithm> algorithmGroup;

    // 抽奖算法阈值，在多少范围内开始选择不同选择
    private final Integer ALGORITHM_THRESHOLD_VALUE = 10000;

    public StrategyArmoryDispatch(Map<String, IAlgorithm> algorithmGroup) {
        this.algorithmGroup = algorithmGroup;
    }


    @Override
    protected void armoryAlgorithm(String key, List<StrategyAwardEntity> strategyAwardList) {
        // 选择不同的抽奖算法进行装配
        BigDecimal minAwardRate = minAwardRate(strategyAwardList);
        // 概率范围值
        double rateRange = convert(minAwardRate.doubleValue());
        // 根据概率范围值选择抽奖算法
        if(rateRange <= ALGORITHM_THRESHOLD_VALUE){
            IAlgorithm o1Algorithm = algorithmGroup.get(AbstractAlgorithm.Algorithm.O1.getKey());
            o1Algorithm.armoryAlgorithm(key, strategyAwardList, BigDecimal.valueOf(rateRange));
            repository.cacheStrategyArmoryAlgorithm(key, AbstractAlgorithm.Algorithm.O1.getKey());
        }else {
            IAlgorithm oLogNAlgorithm = algorithmGroup.get(AbstractAlgorithm.Algorithm.OLogN.getKey());
            oLogNAlgorithm.armoryAlgorithm(key, strategyAwardList, BigDecimal.valueOf(rateRange));
            repository.cacheStrategyArmoryAlgorithm(key, AbstractAlgorithm.Algorithm.OLogN.getKey());
        }
    }

    @Override
    protected Integer dispatchAlgorithm(String key) {
        // 获取缓存的抽奖算法
        String algorithmName = repository.queryStrategyArmoryAlgorithm(key);
        if (null == algorithmName) throw new RuntimeException("key " + key + " algorithmName is " + algorithmName);
        IAlgorithm algorithm = algorithmGroup.get(algorithmName);
        return algorithm.dispatchAlgorithm(key);
    }
}
