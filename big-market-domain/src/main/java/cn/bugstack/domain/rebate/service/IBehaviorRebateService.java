package cn.bugstack.domain.rebate.service;

import cn.bugstack.domain.rebate.model.entity.BehaviorEntity;

import java.util.List;

public interface IBehaviorRebateService {
    // 创建订单
    List<String> createOrder(BehaviorEntity behaviorEntity);
}
