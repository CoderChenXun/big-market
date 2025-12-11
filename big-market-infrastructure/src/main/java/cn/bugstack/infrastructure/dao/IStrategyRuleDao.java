package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.StrategyRule;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IStrategyRuleDao {
    StrategyRule queryStrategyRule(StrategyRule strategyRuleReq);

    String queryStrategyRuleValue(StrategyRule strategyRuleReq);
}
