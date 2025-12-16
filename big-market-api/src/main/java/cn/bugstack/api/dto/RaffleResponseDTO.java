package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleResponseDTO {
    /**
     * 奖品ID
     */
    private Integer awardId;

    /**
     * 奖品顺序
     */
    private Integer awardIndex;
}
