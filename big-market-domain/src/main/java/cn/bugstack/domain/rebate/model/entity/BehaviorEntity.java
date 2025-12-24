package cn.bugstack.domain.rebate.model.entity;

import cn.bugstack.domain.rebate.model.valobj.BehaviorTypeVO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BehaviorEntity {
    // 用户Id
    private String userId;
    // 行为类型
    private BehaviorTypeVO behaviorTypeVO;
    // 业务ID；签到则是日期字符串，支付则是外部的业务ID
    private String outBusinessNo;
}
