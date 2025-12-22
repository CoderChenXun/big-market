package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.task.model.entity.TaskEntity;
import cn.bugstack.domain.task.repository.ITaskRepository;
import cn.bugstack.infrastructure.dao.ITaskDao;
import cn.bugstack.infrastructure.dao.po.Task;
import cn.bugstack.infrastructure.event.EventPublisher;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import javax.annotation.Resource;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Repository
public class TaskRepository implements ITaskRepository {
    @Resource
    private ITaskDao taskDao;

    @Resource
    private EventPublisher eventPublisher;

    @Override
    public List<TaskEntity> queryNoSendMessageTaskList() {
        List<Task> taskList = taskDao.queryNoSendMessageTaskList();
        List<TaskEntity> taskEntityList = taskList.stream().map(task -> TaskEntity.builder()
                .userId(task.getUserId())
                .topic(task.getTopic())
                .messageId(task.getMessageId())
                .message(task.getMessage())
                .build())
                .collect(Collectors.toList());
        return taskEntityList;
    }

    @Override
    public void sendAwardMessage(TaskEntity taskEntity) {
        eventPublisher.publish(taskEntity.getTopic(), taskEntity.getMessage());
    }

    @Override
    public void updateTaskSendMessageCompleted(String userId, String messageId) {
        // 更新任务状态
        Task taskReq = new Task();
        taskReq.setUserId(userId);
        taskReq.setMessageId(messageId);
        taskDao.updateTaskSendMessageCompleted(taskReq);
    }

    @Override
    public void updateTaskSendMessageFail(String userId, String messageId) {
        Task taskReq = new Task();
        taskReq.setUserId(userId);
        taskReq.setMessageId(messageId);
        taskDao.updateTaskSendMessageFail(taskReq);
    }
}
