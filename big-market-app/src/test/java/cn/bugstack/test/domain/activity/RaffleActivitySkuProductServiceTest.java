package cn.bugstack.test.domain.activity;

import cn.bugstack.domain.activity.model.entity.SkuProductEntity;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuProductService;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

/**
 * @Author: coderLan
 * @Description:
 * @DateTime: 2025-12-30 21:21
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class RaffleActivitySkuProductServiceTest {

    @Resource
    private IRaffleActivitySkuProductService raffleActivitySkuProductService;

    @Test
    public void test_querySkuProductEntityListByActivityId() {
        Long activityId = 100301L;
        log.info("查询商品列表开始 activityId:{}", activityId);
        List<SkuProductEntity> skuProductEntities = raffleActivitySkuProductService.querySkuProductEntityListByActivityId(activityId);
        log.info("查询商品列表结束 activityId:{} skuProductEntities:{}", activityId, JSON.toJSONString(skuProductEntities));
    }
}
