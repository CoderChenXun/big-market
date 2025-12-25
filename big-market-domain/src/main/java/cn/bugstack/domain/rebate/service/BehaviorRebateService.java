package cn.bugstack.domain.rebate.service;

import cn.bugstack.domain.rebate.event.SendRebateMessageEvent;
import cn.bugstack.domain.rebate.model.aggregate.BehaviorRebateAggregate;
import cn.bugstack.domain.rebate.model.entity.BehaviorEntity;
import cn.bugstack.domain.rebate.model.entity.BehaviorRebateOrderEntity;
import cn.bugstack.domain.rebate.model.entity.TaskEntity;
import cn.bugstack.domain.rebate.model.valobj.BehaviorTypeVO;
import cn.bugstack.domain.rebate.model.valobj.DailyBehaviorRebateVo;
import cn.bugstack.domain.rebate.model.valobj.TaskStateVO;
import cn.bugstack.domain.rebate.repository.IBehaviorRebateRepository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BehaviorRebateService implements IBehaviorRebateService {

    private IBehaviorRebateRepository behaviorRebateRepository;

    private SendRebateMessageEvent sendRebateMessageEvent;

    // 构造器注入
    public BehaviorRebateService(IBehaviorRebateRepository behaviorRebateRepository, SendRebateMessageEvent sendRebateMessageEvent) {
        this.behaviorRebateRepository = behaviorRebateRepository;
        this.sendRebateMessageEvent = sendRebateMessageEvent;
    }

    @Override
    public List<String> createOrder(BehaviorEntity behaviorEntity) {
        String userId = behaviorEntity.getUserId();
        BehaviorTypeVO behaviorType = behaviorEntity.getBehaviorTypeVO();
        String outBusinessNo = behaviorEntity.getOutBusinessNo();
        // 1. 根据行为类型查询返利配置
        List<DailyBehaviorRebateVo> dailyBehaviorRebateList = behaviorRebateRepository.queryDailyBehaviorRebateByBehaviorType(behaviorType);
        if (null == dailyBehaviorRebateList || dailyBehaviorRebateList.isEmpty()) {
            return new ArrayList<>();
        }

        // 构建聚合对象列表
        List<String> orderIds = new ArrayList<>();
        List<BehaviorRebateAggregate> behaviorRebateAggregateList = new ArrayList<>();
        for (DailyBehaviorRebateVo dailyBehaviorRebate : dailyBehaviorRebateList) {
            // 拼装业务Id：用户Id_返利类型_外部透传业务Id
            String bizId = userId + Constants.UNDERLINE + dailyBehaviorRebate.getRebateType() + Constants.UNDERLINE + outBusinessNo;
            // 构建行为返利订单
            BehaviorRebateOrderEntity behaviorRebateOrderEntity = new BehaviorRebateOrderEntity();
            behaviorRebateOrderEntity.setUserId(userId);
            behaviorRebateOrderEntity.setOrderId(RandomStringUtils.randomNumeric(12));
            behaviorRebateOrderEntity.setBehaviorType(dailyBehaviorRebate.getBehaviorType());
            behaviorRebateOrderEntity.setRebateDesc(dailyBehaviorRebate.getRebateDesc());
            behaviorRebateOrderEntity.setRebateType(dailyBehaviorRebate.getRebateType());
            behaviorRebateOrderEntity.setRebateConfig(dailyBehaviorRebate.getRebateConfig());
            // 设置outBusinessNo
            behaviorRebateOrderEntity.setOutBusinessNo(outBusinessNo);
            // 设置业务ID
            behaviorRebateOrderEntity.setBizId(bizId);
            orderIds.add(behaviorRebateOrderEntity.getOrderId());

            // 构建任务对象
            SendRebateMessageEvent.RebateMessage sendRebateMessage = new SendRebateMessageEvent.RebateMessage();
            sendRebateMessage.setUserId(userId);
            sendRebateMessage.setRebateDesc(dailyBehaviorRebate.getRebateDesc());
            sendRebateMessage.setRebateType(dailyBehaviorRebate.getRebateType());
            sendRebateMessage.setRebateConfig(dailyBehaviorRebate.getRebateConfig());
            sendRebateMessage.setBizId(bizId);


            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> rebateMessageEventMessage = sendRebateMessageEvent.buildEventMessage(sendRebateMessage);
            TaskEntity taskEntity = new TaskEntity();
            taskEntity.setUserId(userId);
            taskEntity.setTopic(sendRebateMessageEvent.topic());
            taskEntity.setMessageId(rebateMessageEventMessage.getId());
            taskEntity.setMessage(rebateMessageEventMessage);
            taskEntity.setState(TaskStateVO.create);

            // 构建聚合对象
            BehaviorRebateAggregate behaviorRebateAggregate = new BehaviorRebateAggregate();
            behaviorRebateAggregate.setUserId(userId);
            behaviorRebateAggregate.setBehaviorRebateOrderEntity(behaviorRebateOrderEntity);
            behaviorRebateAggregate.setTaskEntity(taskEntity);
            behaviorRebateAggregateList.add(behaviorRebateAggregate);
        }

        // 事务性保存聚合对象
        behaviorRebateRepository.saveBehaviorRebateAggregate(userId, behaviorRebateAggregateList);
        return orderIds;
    }

    @Override
    public List<BehaviorRebateOrderEntity> queryOrderByOutBusinessNo(String userId, String outBusinessNo) {
        return behaviorRebateRepository.queryOrderByOutBusinessNo(userId, outBusinessNo);
    }
}
