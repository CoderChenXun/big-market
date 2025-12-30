package cn.bugstack.test.infrastructure;

import cn.bugstack.domain.activity.model.entity.SkuRechargeEntity;
import cn.bugstack.domain.activity.model.entity.UnpaidActivityOrderEntity;
import cn.bugstack.domain.activity.service.quota.RaffleActivityAccountQuotaService;
import cn.bugstack.domain.activity.service.armory.IActivityArmory;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RaffleActivityServiceTest {

    @Resource
    private RaffleActivityAccountQuotaService raffleActivityService;

    @Resource
    private IActivityArmory activityArmory;

    @Before
    public void test_assembleActivitySku() {
        log.info("装配活动sku：assembleActivitySku：{}", 9011L);
        boolean result = activityArmory.assembleActivitySku(9011L);
        log.info("活动装配结果结果：{}", result);
    }

    @Test
    public void test_createSkuRechargeOrder() throws InterruptedException {
        try {
            for (int i = 0; i < 20; i++) {
                SkuRechargeEntity skuRechargeEntity = new SkuRechargeEntity();
                skuRechargeEntity.setUserId("xiaofuge");
                skuRechargeEntity.setSku(9011L);
                skuRechargeEntity.setOutBusinessNo(RandomStringUtils.randomNumeric(12));
                UnpaidActivityOrderEntity unpaidActivityOrderEntity = raffleActivityService.createSkuRechargeOrder(skuRechargeEntity);
                log.info("测试结果：{}", JSON.toJSONString(unpaidActivityOrderEntity));
            }
        }catch (AppException e){
            log.warn("测试异常：{}", e.getInfo());
        }


        new CountDownLatch(1).await();
    }
}
