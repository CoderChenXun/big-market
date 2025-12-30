package cn.bugstack.test.domain.activity;

import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.model.valobj.OrderTradeTypeVO;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.activity.service.armory.IActivityArmory;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityAccountQuotaTest {

    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;
    @Resource
    private IActivityArmory activityArmory;

    @Before
    public void setUp() {
        log.info("装配活动：{}", activityArmory.assembleActivitySku(9011L));
    }

    @Test
    public void test_createSkuRechargeOrder() {
        // 对sku商品进行下单
        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("xiaofuge");
        skuRechargeEntity.setSku(9011L);
        skuRechargeEntity.setOutBusinessNo("700091009113");
        String activityId = raffleActivityAccountQuotaService.createSkuRechargeOrder(skuRechargeEntity);
        log.info("测试结果：{}", activityId);
    }

    @Test
    public void test_credit_pay_order() {
        // 对sku商品进行下单
        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("xiaofuge");
        skuRechargeEntity.setSku(9011L);
        skuRechargeEntity.setOutBusinessNo("251230009113");
        // 积分支付
        skuRechargeEntity.setTradeType(OrderTradeTypeVO.credit_pay_trade);
        String activityId = raffleActivityAccountQuotaService.createSkuRechargeOrder(skuRechargeEntity);
        log.info("测试结果：{}", activityId);
    }
}
