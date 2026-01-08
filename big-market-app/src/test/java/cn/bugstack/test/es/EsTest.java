package cn.bugstack.test.es;

import cn.bugstack.api.IRaffleActivityService;
import cn.bugstack.api.dto.ActivityDrawRequestDTO;
import cn.bugstack.api.dto.ActivityDrawResponseDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.activity.service.armory.IActivityArmory;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.concurrent.CountDownLatch;

/**
 * @Author: coderLan
 * @Description: 测试
 * @DateTime: 2026-01-08 10:24
 **/
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class EsTest {

    @Resource
    private IRaffleActivityService raffleActivityService;

    @Resource
    private IActivityArmory activityArmory;

    @Before
    public void test_armory(){
        Response<Boolean> response = raffleActivityService.armory(100301L);
        log.info("测试结果：{}", JSON.toJSONString(response));
    }

    @Test
    public void test_draw() {
        for (int i = 0; i < 10; i++) {
            ActivityDrawRequestDTO request = new ActivityDrawRequestDTO();
            request.setActivityId(100301L);
            request.setUserId("xiaofuge");
            Response<ActivityDrawResponseDTO> response = raffleActivityService.draw(request);
            log.info("请求参数：{}", JSON.toJSONString(request));
            log.info("测试结果：{}", JSON.toJSONString(response));
        }
    }

    @Test
    public void test_calendarSignRebate() throws InterruptedException {
        Response<Boolean> response = raffleActivityService.calendarSignRebate("xiaofuge01");
        log.info("测试结果：{}", JSON.toJSONString(response));
        // 让程序挺住方便测试，也可以去掉
        new CountDownLatch(1).await();
    }
}
