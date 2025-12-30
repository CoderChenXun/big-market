package cn.bugstack.api.dto;

import lombok.Data;

/**
 * @Author: coderLan
 * @Description: 购物车商品信息
 * @DateTime: 2025-12-30 19:51
 **/
@Data
public class SkuProductShopCartRequestDTO {
    /**
     * 用户 ID
     */
    private String userId;
    /**
     * 商品 ID
     */
    private Long sku;
}
