package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityOrderEntity;
import cn.bugstack.domain.activity.model.entity.ActivityShopCartEntity;

public interface IRaffleOrder {
    /**
     * 根据购物车activityShopCartEntity实体类创建抽奖活动订单
     * @param activityShopCartEntity
     * @return
     */
    ActivityOrderEntity createRaffleActivityOrder(ActivityShopCartEntity activityShopCartEntity);
}
