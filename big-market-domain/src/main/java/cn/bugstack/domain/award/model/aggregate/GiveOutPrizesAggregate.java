package cn.bugstack.domain.award.model.aggregate;

import cn.bugstack.domain.award.model.entity.UserAwardRecordEntity;
import cn.bugstack.domain.award.model.entity.UserCreditAwardEntity;
import cn.bugstack.domain.award.model.valobj.AwardStateVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-26 16:30
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GiveOutPrizesAggregate {
    /**
     * 用户 ID
     */
    private String userId;
    /**
     * 用户发奖记录
     */
    private UserAwardRecordEntity userAwardRecordEntity;
    /**
     * 用户积分奖品
     */
    private UserCreditAwardEntity userCreditAwardEntity;

    public static UserAwardRecordEntity buildUserAwardRecordEntity(String userId, String orderId, Integer awardId, AwardStateVO awardState) {
        return UserAwardRecordEntity.builder()
                .userId(userId)
                .orderId(orderId)
                .awardId(awardId)
                .awardState(awardState)
                .build();
    }

    public static UserCreditAwardEntity buildUserCreditAwardEntity(String userId, BigDecimal creditMount) {
        return UserCreditAwardEntity.builder()
                .userId(userId)
                .creditMount(creditMount)
                .build();
    }
}
