package cn.bugstack.api;

import cn.bugstack.api.dto.ActivityDrawRequestDTO;
import cn.bugstack.api.dto.ActivityDrawResponseDTO;
import cn.bugstack.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.api.response.Response;

import java.util.List;

public interface IRaffleActivityService {

    /**
     * 活动和抽奖策略装配接口
     *
     * @param activityId
     * @return
     */
    Response<Boolean> armory(Long activityId);

    /**
     * 抽奖
     *
     * @param request
     * @return
     */
    Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO request);

    /**
     * 查询奖品列表
     */
    Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(RaffleAwardListRequestDTO request);
}
