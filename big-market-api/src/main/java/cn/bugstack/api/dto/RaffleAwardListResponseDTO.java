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
public class RaffleAwardListResponseDTO implements Serializable {

    private static final long serialVersionUID = -1511698631907583682L;
    /**
     * 奖品ID
     */
    private Integer awardId;

    /**
     * 奖品标题
     */
    private String awardTitle;

    /**
     * 奖品子标题
     */
    private String awardSubtitle;

    /**
     * 奖品顺序
     */
    private Integer sort;

    // 奖品次数规则 -- 抽奖N次后解锁，未配置则为空
    private Integer awardRuleLockCount;

    // 奖品是否解锁
    private Boolean isAwardUnlock;

    // 等待解锁的次数
    private Integer waitUnlockCount;
}
