package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.SkuProductEntity;

import java.util.List;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-30 20:50
 **/
public interface IRaffleActivitySkuProductService {

    List<SkuProductEntity> querySkuProductEntityListByActivityId(Long activityId);
}
