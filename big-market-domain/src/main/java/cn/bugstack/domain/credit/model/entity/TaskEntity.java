package cn.bugstack.domain.credit.model.entity;

import cn.bugstack.domain.award.model.valobj.TaskStateVO;
import cn.bugstack.domain.credit.model.event.CreditAdjustSuccessMessageEvent;
import cn.bugstack.types.event.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 21:26
 **/
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {

    /**
     * 活动ID
     */
    private String userId;
    /**
     * 消息主题
     */
    private String topic;
    /**
     * 消息编号
     */
    private String messageId;
    /**
     * 消息主体
     */
    private BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> message;
    /**
     * 任务状态；create-创建、completed-完成、fail-失败
     */
    private TaskStateVO state;

}
