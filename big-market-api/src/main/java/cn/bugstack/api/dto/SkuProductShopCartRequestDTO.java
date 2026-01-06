package cn.bugstack.api.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Author: coderLan
 * @Description: 购物车商品信息
 * @DateTime: 2025-12-30 19:51
 **/
@Data
public class SkuProductShopCartRequestDTO implements Serializable {

    private static final long serialVersionUID = 1138080622828392066L;
    /**
     * 用户 ID
     */
    private String userId;
    /**
     * 商品 ID
     */
    private Long sku;
}
