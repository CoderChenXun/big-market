package cn.bugstack.api;

import cn.bugstack.api.dto.*;
import cn.bugstack.api.response.Response;

import java.math.BigDecimal;
import java.util.List;

public interface IRaffleActivityService {

    /**
     * 活动和抽奖策略装配接口
     *
     * @param activityId
     * @return
     */
    Response<Boolean> armory(Long activityId);

    /**
     * 抽奖
     *
     * @param request
     * @return
     */
    Response<ActivityDrawResponseDTO> draw(ActivityDrawRequestDTO request);

    Response<Boolean> calendarSignRebate(String userId);

    Response<Boolean> isCalendarSignRebate(String userId);

    Response<UserActivityAccountResponseDTO> queryUserActivityAccountEntity(UserActivityAccountRequestDTO request);

    /**
     * 积分支付交换sku订单
     *
     * @param request
     * @return 交换结果
     */
    Response<Boolean> creditPayExchangeSku(SkuProductShopCartRequestDTO request);

    /**
     * 查询用户积分账户
     *
     * @param userId
     * @return 用户积分账户的剩余积分
     */
    Response<BigDecimal> queryUserCreditAccount(String userId);

    /**
     * 查询活动商品列表
     *
     * @param activityId
     * @return 商品列表
     */
    Response<List<SkuProductResponseDTO>> querySkuProductListByActivityId(Long activityId);
}
