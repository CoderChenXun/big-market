package cn.bugstack.api.dto;

import lombok.Data;

@Data
public class RaffleAwardListRequestDTO {
    /**
     * 用户Id
     */
    private String userId;
    /**
     * 活动Id
     */
    private Long activityId;
}
