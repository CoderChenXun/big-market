package cn.bugstack.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-25 14:47
 **/
@Data
public class RaffleStrategyRuleWeightRequestDTO implements Serializable {

    private static final long serialVersionUID = 8160100287780883130L;
    /**
     * 用户Id
     */
    private String userId;
    /**
     * 活动Id
     */
    private Long activityId;
}