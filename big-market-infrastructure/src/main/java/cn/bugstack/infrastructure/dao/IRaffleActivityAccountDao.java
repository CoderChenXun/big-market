package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.RaffleActivityAccount;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IRaffleActivityAccountDao {
    int updateAccountQuota(RaffleActivityAccount raffleActivityAccount);

    void insert(RaffleActivityAccount raffleActivityAccount);
}
