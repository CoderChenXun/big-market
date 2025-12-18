package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.domain.activity.service.rule.factory.DefaultActivityChainFactory;

public class RaffleActivitySupport {
    protected DefaultActivityChainFactory activityChainFactory;

    protected IActivityRepository activityRepository;

    // 构造器注入活动仓储
    public RaffleActivitySupport(DefaultActivityChainFactory activityChainFactory, IActivityRepository activityRepository) {
        this.activityChainFactory = activityChainFactory;
        this.activityRepository = activityRepository;
    }

    public ActivitySkuEntity queryActivitySku(Long sku) {
        return activityRepository.queryActivitySku(sku);
    }

    public ActivityEntity queryRaffleActivityByActivityId(Long activityId) {
        return activityRepository.queryRaffleActivityByActivityId(activityId);
    }

    public ActivityCountEntity queryRaffleActivityCountByActivityCountId(Long activityCountId) {
        return activityRepository.queryRaffleActivityCountByActivityCountId(activityCountId);
    }
}
