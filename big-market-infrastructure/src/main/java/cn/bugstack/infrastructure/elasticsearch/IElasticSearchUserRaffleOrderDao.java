package cn.bugstack.infrastructure.elasticsearch;

import cn.bugstack.infrastructure.elasticsearch.po.UserRaffleOrder;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2026-01-08 15:01
 **/
@Mapper
public interface IElasticSearchUserRaffleOrderDao {

    List<UserRaffleOrder> queryUserRaffleOrderList();
}
