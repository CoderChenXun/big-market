package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.UserCreditAccount;
import org.apache.ibatis.annotations.Mapper;

/**
 * @Author: coderLan
 * @Description: 用户积分账户Dao
 * @DateTime: 2025-12-26 16:28
 **/
@Mapper
public interface IUserCreditAccountDao {
    int updateAddAmount(UserCreditAccount userCreditAccount);

    void insert(UserCreditAccount userCreditAccount);

    UserCreditAccount queryUserCreditAccount(UserCreditAccount userCreditAccountReq);
}
