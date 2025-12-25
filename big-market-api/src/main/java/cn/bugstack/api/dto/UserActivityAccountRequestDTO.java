package cn.bugstack.api.dto;

import lombok.Data;

/**
 * @Author: coderLan
 * @Description: 查询用户活动账户信息请求参数
 * @DateTime: 2025-12-25 14:14
 **/
@Data
public class UserActivityAccountRequestDTO {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 活动ID
     */
    private Long activityId;
}
