package cn.bugstack.domain.award.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: coderLan
 * @Description: 用户发奖实体
 * @DateTime: 2025-12-26 15:59
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DistributeAwardEntity {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 奖品ID
     */
    private Integer awardId;
    /**
     * 订单Id
     */
    private String orderId;
    /**
     * 奖品配置信息
     */
    private String awardConfig;
}
