package cn.bugstack.domain.rebate.repository;

import cn.bugstack.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.bugstack.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.bugstack.domain.rebate.model.valobj.DailyBehaviorRebateVo;

import java.util.List;

public interface IBehaviorRebateRepository {
    List<DailyBehaviorRebateVo> queryDailyBehaviorRebateByBehaviorType(BehaviorTypeVO behaviorType);

    void saveBehaviorRebateAggregate(String userId, List<BehaviorRebateAggregate> behaviorRebateAggregateList);
}
