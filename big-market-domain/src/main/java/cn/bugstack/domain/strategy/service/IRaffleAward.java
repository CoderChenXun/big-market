package cn.bugstack.domain.strategy.service;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;

import java.util.List;

public interface IRaffleAward {

    /**
     * 根据策略ID查询抽奖奖品列表配置
     */
    List<StrategyAwardEntity> queryStrategyAwardListByStrategyId(Long strategyId);

    List<StrategyAwardEntity> queryStrategyAwardListByActivityId(Long activityId);

    List<StrategyAwardStockKeyVO> queryOpenActivityStrategyAwardList();
}
