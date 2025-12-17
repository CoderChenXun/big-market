package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.RaffleActivityCountFlow;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DBRouterStrategy(splitTable = true)
public interface IRaffleActivityCountFlowDao {

    // 插入抽奖活动流水数据
    @DBRouter(key = "userId")
    void insert(RaffleActivityCountFlow flow);

    // 根据用户ID查询抽奖活动流水数据
    @DBRouter
    List<RaffleActivityCountFlow> queryActivityOrderByUserId(String userId);
}
