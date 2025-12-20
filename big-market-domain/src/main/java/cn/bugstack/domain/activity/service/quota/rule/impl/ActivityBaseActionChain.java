package cn.bugstack.domain.activity.service.quota.rule.impl;

import cn.bugstack.domain.activity.model.entity.ActivityCountEntity;
import cn.bugstack.domain.activity.model.entity.ActivityEntity;
import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.model.valobj.ActivityStateVO;
import cn.bugstack.domain.activity.service.quota.rule.AbstractActionChain;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Date;

@Slf4j
@Component("activity_base_action")
public class ActivityBaseActionChain extends AbstractActionChain {
    // 活动基础信息校验，校验简单的有效期及活动状态
    @Override
    public Boolean action(ActivityEntity activityEntity, ActivitySkuEntity activitySkuEntity, ActivityCountEntity activityCountEntity) {
        log.info("活动责任链-基础信息【有效期、状态、库存(sku)】校验开始。sku:{} activityId:{}", activitySkuEntity.getSku(), activityEntity.getActivityId());

        // 校验活动状态
        if(!activityEntity.getState().equals(ActivityStateVO.open)){
            log.info("活动责任链-基础信息校验失败：活动不处于开启状态，state：{}", activityEntity.getState());
            throw new AppException(ResponseCode.ACTIVITY_STATE_ERROR.getCode(), ResponseCode.ACTIVITY_STATE_ERROR.getInfo());
        }

        // 校验时间的有效期
        Date currentDateTime = new Date();
        if(currentDateTime.before(activityEntity.getBeginDateTime()) || currentDateTime.after(activityEntity.getEndDateTime())){
            log.info("活动责任链-基础信息校验失败：活动已过期。");
            throw new AppException(ResponseCode.ACTIVITY_DATE_ERROR.getCode(), ResponseCode.ACTIVITY_DATE_ERROR.getInfo());
        }

        // 校验库存
        if(activitySkuEntity.getStockCountSurplus() <= 0){
            throw new AppException(ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getCode(), ResponseCode.ACTIVITY_SKU_STOCK_ERROR.getInfo());
        }

        return next().action(activityEntity, activitySkuEntity, activityCountEntity);
    }
}
