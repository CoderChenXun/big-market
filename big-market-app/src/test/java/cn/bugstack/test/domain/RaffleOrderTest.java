package cn.bugstack.test.domain;

import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleOrderTest {

    @Resource
    private IRaffleActivityAccountQuotaService raffleOrder;

    @Test
    public void test_createSkuRechargeOrder() {
        SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
        skuRechargeEntity.setUserId("xiaofuge");
        skuRechargeEntity.setSku(9011L);
        skuRechargeEntity.setOutBusinessNo("700091009113");
        String activityId = raffleOrder.createSkuRechargeOrder(skuRechargeEntity);
        log.info("测试结果：{}", activityId);
    }
}
