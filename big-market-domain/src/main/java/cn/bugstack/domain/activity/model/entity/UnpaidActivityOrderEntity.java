package cn.bugstack.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Date;

/**
 * @Author: coderLan
 * @Description: 未支付订单
 * @DateTime: 2025-12-30 20:15
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UnpaidActivityOrderEntity {
    /**
     * 用户ID
     */
    private String userId;

    /**
     * 订单ID
     */
    private String orderId;

    /**
     * 支付金额【积分】
     */
    private BigDecimal payAmount;

    /**
     * 业务仿重ID - 外部透传的，确保幂等
     */
    private String outBusinessNo;
}
