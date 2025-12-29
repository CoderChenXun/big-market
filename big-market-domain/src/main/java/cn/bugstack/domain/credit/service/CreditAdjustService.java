package cn.bugstack.domain.credit.service;

import cn.bugstack.domain.credit.model.aggregate.TradeAggregate;
import cn.bugstack.domain.credit.model.entity.CreditAccountEntity;
import cn.bugstack.domain.credit.model.entity.CreditOrderEntity;
import cn.bugstack.domain.credit.model.entity.TradeEntity;
import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import cn.bugstack.domain.credit.repository.ICreditRepository;
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

    public CreditAdjustService(ICreditRepository creditRepository) {
        this.creditRepository = creditRepository;
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

        CreditAccountEntity creditAccountEntity = TradeAggregate.buildCreditAccountEntity(userId, amount);
        CreditOrderEntity creditOrderEntity = TradeAggregate.buildCreditOrderEntity(userId, tradeName, tradeType, amount, outBusinessNo);
        TradeAggregate tradeAggregate = TradeAggregate.builder()
                .userId(userId)
                .creditAccountEntity(creditAccountEntity)
                .creditOrderEntity(creditOrderEntity)
                .build();

        creditRepository.saveUserCreditTradeOrder(tradeAggregate);
        log.info("增加账户积分额度完成 userId:{} tradeName:{} amount:{}", tradeEntity.getUserId(), tradeEntity.getTradeName(), tradeEntity.getAmount());
        return creditOrderEntity.getOrderId();
    }
}
