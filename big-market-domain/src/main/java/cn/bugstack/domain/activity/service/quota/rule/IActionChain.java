package cn.bugstack.domain.activity.service.quota.rule;

import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;

public interface IActionChain extends IActionChainArmory {

    // 定义核心逻辑过滤操作
    Boolean action(ActivityEntity activityEntity, ActivitySkuEntity activitySkuEntity, ActivityCountEntity activityCountEntity);
}
