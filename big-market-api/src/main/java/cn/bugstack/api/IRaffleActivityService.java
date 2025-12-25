package cn.bugstack.api;

import cn.bugstack.api.dto.*;
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

    Response<Boolean> calendarSignRebate(String userId);

    Response<Boolean> isCalendarSignRebate(String userId);

    Response<UserActivityAccountResponseDTO> queryUserActivityAccountEntity( UserActivityAccountRequestDTO request);

}
