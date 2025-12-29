package cn.bugstack.domain.credit.model.entity;

import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 15:53
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TradeEntity {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 交易名称
     */
    private TradeNameVO tradeName;
    /**
     * 交易类型--增加积分、减少积分
     */
    private TradeTypeVO tradeType;
    /**
     * 交易积分值
     */
    private BigDecimal amount;
    /**
     * 业务仿重ID - 外部透传。返利、行为等唯一标识
     */
    private String outBusinessNo;
}
