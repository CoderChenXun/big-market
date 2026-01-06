package cn.bugstack.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class ActivityDrawRequestDTO implements Serializable {
    /**
     * 序列化ID
     */
    private static final long serialVersionUID = 6457850745215896294L;
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 活动ID
     */
    private Long activityId;
}
