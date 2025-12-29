package cn.bugstack.domain.credit.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 16:06
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreditAccountEntity {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 调整额度
     */
    private BigDecimal adjustAmount;
}
