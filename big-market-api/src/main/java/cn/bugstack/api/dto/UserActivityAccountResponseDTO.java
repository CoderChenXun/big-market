package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: coderLan
 * @Description: 查询用户抽奖账户的返回结果
 * @DateTime: 2025-12-25 14:14
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserActivityAccountResponseDTO {
    /**
     * 用户抽奖总次数
     */
    private Integer totalCount;
    /**
     * 用户总次数剩余
     */
    private Integer totalCountSurplus;
    /**
     * 用户日次数
     */
    private Integer dayCount;
    /**
     * 用户日次数剩余
     */
    private Integer dayCountSurplus;
    /**
     * 用户月次数
     */
    private Integer monthCount;
    /**
     * 用户月次数剩余
     */
    private Integer monthCountSurplus;
}
