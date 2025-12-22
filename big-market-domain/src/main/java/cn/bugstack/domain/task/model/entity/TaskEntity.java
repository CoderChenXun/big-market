package cn.bugstack.domain.task.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TaskEntity {
    /**
     * 用户ID
     */
    private String userId;
    /**
     * 消息主题
     */
    private String topic;
    /**
     * 消息ID
     */
    private String messageId;
    /**
     * 消息主体
     */
    private String message;

}
