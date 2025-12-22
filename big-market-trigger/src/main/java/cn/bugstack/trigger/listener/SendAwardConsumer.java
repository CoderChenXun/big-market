package cn.bugstack.trigger.listener;

import cn.bugstack.domain.award.event.SendAwardMessageEvent;
import cn.bugstack.types.event.BaseEvent;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Slf4j
@Component
public class SendAwardConsumer {

    @Value("${spring.rabbitmq.topic.send_award}")
    private String topic;


    @RabbitListener(queuesToDeclare = @Queue(value = "${spring.rabbitmq.topic.send_award}"))
    public void listener(String  message){
        try {
            log.info("监听奖品发送消息 topic: {} message: {}", topic, message);
            BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendAwardMessageEvent.SendAwardMessage>>() {
            }.getType());
            SendAwardMessageEvent.SendAwardMessage sendAwardMessage = eventMessage.getData();
            // 进行奖品发送
            log.info("进行奖品发送，用户ID: {}, 奖品ID: {}, 奖品标题: {}", sendAwardMessage.getUserId(), sendAwardMessage.getAwardId(), sendAwardMessage.getAwardTitle());
        }catch (Exception e) {
            log.error("监听奖品发送消息，消费失败 topic: {} message: {}", topic, message);
            throw e;
        }
    }
}
