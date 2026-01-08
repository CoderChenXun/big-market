package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.Award;
import cn.bugstack.infrastructure.dao.po.StrategyAward;
import org.apache.ibatis.annotations.Mapper;
import java.util.List;


@Mapper
public interface IStrategyAwardDao {
    List<StrategyAward> queryStrategyAwardListByStrategyId(Long strategyId);

    StrategyAward queryStrategyAwardRuleModels(StrategyAward strategyAwardReq);

    void updateStrategyAwardStock(StrategyAward strategyAwardReq);

    StrategyAward queryStrategyAwardEntity(StrategyAward strategyAwardReq);

    List<StrategyAward> queryStrategyAwardListByAwardIds(List<Integer> awardIds);

    List<StrategyAward> queryOpenActivityStrategyAwardList();
}
