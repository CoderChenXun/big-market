package cn.bugstack.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RaffleResponseDTO implements Serializable {

    private static final long serialVersionUID = 2477961470948519850L;
    /**
     * 奖品ID
     */
    private Integer awardId;

    /**
     * 奖品顺序
     */
    private Integer awardIndex;
}
