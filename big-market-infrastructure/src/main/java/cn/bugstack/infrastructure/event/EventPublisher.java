package cn.bugstack.infrastructure.event;

import cn.bugstack.types.event.BaseEvent;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component
public class EventPublisher {

    @Resource
    private RabbitTemplate rabbitTemplate;

    public void publish(String topic, BaseEvent.EventMessage<?> eventMessage){
        publish(topic,JSON.toJSONString(eventMessage));
    }

    public void publish(String topic, String messageJson){
        try {
            rabbitTemplate.convertAndSend(topic, messageJson);
            log.info("MQ消息发送成功 topic:{} message:{}", topic, messageJson);
        }catch (Exception e){
            log.error("MQ消息发送失败 topic:{} message:{}", topic, messageJson, e);
            throw e;
        }
    }
}
