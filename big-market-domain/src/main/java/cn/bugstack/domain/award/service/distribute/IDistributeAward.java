package cn.bugstack.domain.award.service.distribute;

import cn.bugstack.domain.award.model.entity.DistributeAwardEntity;

/**
 * @Author: coderLan
 * @Description: 发放奖品的接口
 * @DateTime: 2025-12-26 15:56
 **/
public interface IDistributeAward {

    void giveOutPrizes(DistributeAwardEntity distributeAwardEntity);
}
