package cn.bugstack.domain.strategy.service.armory.algorithm;

import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.domain.strategy.service.armory.algorithm.IAlgorithm;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.annotation.Resource;
import java.security.SecureRandom;

/**
 * @Author: coderLan
 * @Description: 抽奖算法抽象类
 * @DateTime: 2026-01-09 11:25
 **/
public abstract class AbstractAlgorithm implements IAlgorithm {

    @Resource
    protected IStrategyRepository repository;

    protected final SecureRandom secureRandom = new SecureRandom();

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    public enum Algorithm {
        O1("o1Algorithm"), OLogN("oLogNAlgorithm");

        private String key;
    }
}
