package cn.bugstack.api.dto;

import lombok.Data;

import java.io.Serializable;

@Data
public class RaffleRequestDTO implements Serializable {

    private static final long serialVersionUID = 3893024166459426119L;
    /**
     * 策略ID
     */
    private Long strategyId;
}
