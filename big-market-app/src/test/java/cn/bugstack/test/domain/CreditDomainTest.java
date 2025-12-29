package cn.bugstack.test.domain;

import cn.bugstack.domain.credit.model.entity.TradeEntity;
import cn.bugstack.domain.credit.model.valobj.TradeNameVO;
import cn.bugstack.domain.credit.model.valobj.TradeTypeVO;
import cn.bugstack.domain.credit.service.ICreditAdjustService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.math.BigDecimal;

/**
 * @Author: coderLan
 * @Description: 积分领域测试
 * @DateTime: 2025-12-29 17:09
 **/
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class CreditDomainTest {
    @Resource
    private ICreditAdjustService creditAdjustService;

    @Test
    public void test_createOrder_Forward() {
        TradeEntity tradeEntity = new TradeEntity();
        tradeEntity.setUserId("xiaofuge");
        tradeEntity.setTradeName(TradeNameVO.REBATE);
        tradeEntity.setTradeType(TradeTypeVO.FORWARD);
        tradeEntity.setAmount(new BigDecimal(100));
        tradeEntity.setOutBusinessNo("20251229217");
        String orderId = creditAdjustService.createOrder(tradeEntity);
        log.info("测试结果：{}", orderId);
    }

    @Test
    public void test_createOrder_Reverse() {
        TradeEntity tradeEntity = new TradeEntity();
        tradeEntity.setUserId("xiaofuge");
        tradeEntity.setTradeName(TradeNameVO.REBATE);
        tradeEntity.setTradeType(TradeTypeVO.REVERSE);
        tradeEntity.setAmount(new BigDecimal("-100"));
        tradeEntity.setOutBusinessNo("20251229218");
        String orderId = creditAdjustService.createOrder(tradeEntity);
        log.info("测试结果：{}", orderId);
    }
}
