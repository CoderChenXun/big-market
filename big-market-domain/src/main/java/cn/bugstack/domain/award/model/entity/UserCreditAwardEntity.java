package cn.bugstack.domain.award.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-26 16:32
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserCreditAwardEntity {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 总积分，显示总账户值，记得一个人获得的总积分
     */
    private BigDecimal creditMount;
}
