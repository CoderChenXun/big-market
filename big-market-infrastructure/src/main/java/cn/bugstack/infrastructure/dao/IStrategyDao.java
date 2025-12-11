package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.Strategy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IStrategyDao {
    Strategy queryStrategyEntityByStrategyId(Long strategyId);
}
