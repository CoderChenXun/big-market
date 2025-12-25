package cn.bugstack.trigger.listener;

import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.award.event.SendAwardMessageEvent;
import cn.bugstack.domain.rebate.event.SendRebateMessageEvent;
import cn.bugstack.domain.rebate.model.valobj.RebateTypeVO;
import cn.bugstack.types.event.BaseEvent;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.TypeReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;


@Slf4j
@Component
public class RebateMessageConsumer {

    @Value("${spring.rabbitmq.topic.send_rebate}")
    private String topic;

    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;


    @RabbitListener(queuesToDeclare = @Queue(value = "${spring.rabbitmq.topic.send_rebate}"))
    public void listener(String message) {
        try {
            log.info("监听用户行为返利发送消息 topic: {} message: {}", topic, message);
            BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage> eventMessage = JSON.parseObject(message, new TypeReference<BaseEvent.EventMessage<SendRebateMessageEvent.RebateMessage>>() {
            }.getType());
            SendRebateMessageEvent.RebateMessage rebateMessage = eventMessage.getData();
            // 进行奖品发送
            String rebateType = rebateMessage.getRebateType();
            if (!RebateTypeVO.SKU.getCode().equals(rebateType)) {
                log.info("监听用户行为返利消息 - 非sku奖励暂时不处理 topic: {} message: {}", topic, message);
                return;
            }
            // 是sku奖励，给对应的用户进行sku奖励
            SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
            skuRechargeEntity.setUserId(rebateMessage.getUserId());
            skuRechargeEntity.setSku(Long.valueOf(rebateMessage.getRebateConfig()));
            skuRechargeEntity.setOutBusinessNo(rebateMessage.getBizId());
            raffleActivityAccountQuotaService.createSkuRechargeOrder(skuRechargeEntity);
        } catch (Exception e) {
            log.error("监听用户行为返利发送消息，消费失败 topic: {} message: {}", topic, message);
            throw e;
        }
    }
}
