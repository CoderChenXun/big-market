package cn.bugstack.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RaffleAwardListRequestDTO implements Serializable {

    private static final long serialVersionUID = 4531462495761922353L;
    /**
     * 用户Id
     */
    private String userId;
    /**
     * 活动Id
     */
    private Long activityId;
}
