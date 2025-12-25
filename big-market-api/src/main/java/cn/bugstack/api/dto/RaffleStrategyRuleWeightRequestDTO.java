package cn.bugstack.api.dto;

import lombok.Data;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-25 14:47
 **/
@Data
public class RaffleStrategyRuleWeightRequestDTO {
    /**
     * 用户Id
     */
    private String userId;
    /**
     * 活动Id
     */
    private Long activityId;
}