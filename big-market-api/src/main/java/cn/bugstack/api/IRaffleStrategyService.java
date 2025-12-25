package cn.bugstack.api;

import cn.bugstack.api.dto.*;
import cn.bugstack.api.response.Response;

import java.util.List;

public interface IRaffleStrategyService {
    /**
     * 策略装配接口
     *
     * @param strategyId
     * @return
     */
    Response<Boolean> strategyArmory(Long strategyId);

    /**
     * 查询抽奖奖品列表
     * @param requestDTO
     * @return
     */
    Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(RaffleAwardListRequestDTO requestDTO);

    /**
     * 抽奖
     *
     * @param requestDTO
     * @return
     */
    Response<RaffleResponseDTO> randomRaffle(RaffleRequestDTO requestDTO);


    Response<List<RaffleStrategyRuleWeightResponseDTO>> queryRaffleStrategyRuleWeight(RaffleStrategyRuleWeightRequestDTO request);
}
