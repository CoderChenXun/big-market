package cn.bugstack.domain.credit.service;

import cn.bugstack.domain.credit.model.aggregate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.entity.TaskEntity;
import cn.bugstack.domain.credit.model.entity.TradeEntity;
import cn.bugstack.domain.credit.model.event.CreditAdjustSuccessMessageEvent;
import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import cn.bugstack.domain.credit.repository.ICreditRepository;
import cn.bugstack.types.event.BaseEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-29 15:51
 **/
@Service
@Slf4j
public class CreditAdjustService implements ICreditAdjustService {
    private ICreditRepository creditRepository;

    private CreditAdjustSuccessMessageEvent creditAdjustSuccessMessageEvent;

    public CreditAdjustService(ICreditRepository creditRepository, CreditAdjustSuccessMessageEvent creditAdjustSuccessMessageEvent) {
        this.creditRepository = creditRepository;
        this.creditAdjustSuccessMessageEvent = creditAdjustSuccessMessageEvent;
    }

    @Override
    public String createOrder(TradeEntity tradeEntity) {
        log.info("增加账户积分额度开始 userId:{} tradeName:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getAmount());
        // 构建聚合对象
        String userId = tradeEntity.getUserId();
        TradeNameVO tradeName = tradeEntity.getTradeName();
        TradeTypeVO tradeType = tradeEntity.getTradeType();
        BigDecimal amount = tradeEntity.getAmount();
        String outBusinessNo = tradeEntity.getOutBusinessNo();

        // 积分账户实体
        CreditAccountEntity creditAccountEntity = TradeAggregate.buildCreditAccountEntity(userId, amount);
        // 积分订单实体
        CreditOrderEntity creditOrderEntity = TradeAggregate.buildCreditOrderEntity(userId, tradeName, tradeType, amount, outBusinessNo);
        // 任务实体
        CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage creditAdjustSuccessMessage = new CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage();
        creditAdjustSuccessMessage.setUserId(userId);
        creditAdjustSuccessMessage.setOrderId(creditOrderEntity.getOrderId());
        creditAdjustSuccessMessage.setAmount(tradeEntity.getAmount());
        creditAdjustSuccessMessage.setOutBusinessNo(tradeEntity.getOutBusinessNo());
        BaseEvent.EventMessage<CreditAdjustSuccessMessageEvent.CreditAdjustSuccessMessage> creditAdjustSuccessMessageEventMessage = creditAdjustSuccessMessageEvent.buildEventMessage(creditAdjustSuccessMessage);
        TaskEntity taskEntity = TradeAggregate.createTaskEntity(userId, creditAdjustSuccessMessageEvent.topic(), creditAdjustSuccessMessageEventMessage.getId(), creditAdjustSuccessMessageEventMessage);
        // 聚合对象
        TradeAggregate tradeAggregate = TradeAggregate.builder()
                .userId(userId)
                .creditAccountEntity(creditAccountEntity)
                .creditOrderEntity(creditOrderEntity)
                .taskEntity(taskEntity)
                .build();

        creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        log.info("增加账户积分额度完成 userId:{} tradeName:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getAmount());
        return creditOrderEntity.getOrderId();
    }

    @Override
    public CreditAccountEntity queryUserCreditAccount(String userId) {
        return creditRepository.queryUserCreditAccount(userId);
    }
}
