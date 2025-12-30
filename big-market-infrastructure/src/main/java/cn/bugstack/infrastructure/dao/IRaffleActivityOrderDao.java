package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.RaffleActivityOrder;
import cn.bugstack.middleware.db.router.annotation.DBRouter;
import cn.bugstack.middleware.db.router.annotation.DBRouterStrategy;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
@DBRouterStrategy(splitTable = true)
public interface IRaffleActivityOrderDao {

    // 插入抽奖活动订单
    @DBRouter(key = "userId")
    void insert(RaffleActivityOrder order);

    // 根据用户ID查询抽奖活动订单
    @DBRouter
    List<RaffleActivityOrder> queryRaffleActivityOrderByUserId(String userId);

    @DBRouter
    RaffleActivityOrder queryRaffleActivityOrder(RaffleActivityOrder raffleActivityOrderReq);

    int updateOrderCompleted(RaffleActivityOrder raffleActivityOrderReq);
}
