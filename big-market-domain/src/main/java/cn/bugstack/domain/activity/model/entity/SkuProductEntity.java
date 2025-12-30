package cn.bugstack.domain.activity.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-30 20:52
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkuProductEntity {
    /**
     * 商品sku
     */
    private Long sku;
    /**
     * 活动ID
     */
    private Long activityId;
    /**
     * 活动个人参数ID；在这个活动上，一个人可参与多少次活动（总、日、月）
     */
    private Long activityCountId;
    /**
     * 库存总量
     */
    private Integer stockCount;
    /**
     * 剩余库存
     */
    private Integer stockCountSurplus;
    /**
     * payAmount 支付金额
     */
    private BigDecimal payAmount;
    /**
     * 活动次数
     */
    private ActivityCount activityCount;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public  static class ActivityCount {
        /**
         * 总次数
         */
        private Integer totalCount;

        /**
         * 日次数
         */
        private Integer dayCount;

        /**
         * 月次数
         */
        private Integer monthCount;
    }
}
