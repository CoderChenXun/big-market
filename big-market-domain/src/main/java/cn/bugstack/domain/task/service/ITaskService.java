package cn.bugstack.domain.task.service;

import cn.bugstack.domain.task.model.entity.TaskEntity;

import java.util.List;

public interface ITaskService {
    // 查询没有发送的消息任务列表
    List<TaskEntity> queryNoSendMessageTaskList();

    void sendAwardMessage(TaskEntity taskEntity);

    void updateTaskSendMessageCompleted(String userId, String messageId);

    void updateTaskSendMessageFail(String userId, String messageId);
}
