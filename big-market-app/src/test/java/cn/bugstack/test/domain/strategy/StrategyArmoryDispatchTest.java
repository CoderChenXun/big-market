package cn.bugstack.test.domain.strategy;

import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.domain.strategy.service.armory.IStrategyDispatch;
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
public class StrategyArmoryDispatchTest {

    @Resource
    private IStrategyArmory strategyArmory;

    @Resource
    private IStrategyDispatch strategyDispatch;


    /**
     * 策略ID；100001L、100002L 装配的时候创建策略表写入到 Redis Map 中
     */
    @Before
    public void test_strategyArmory() {
        boolean success = strategyArmory.assembleLotteryStrategy(100001L);
        log.info("测试结果：{}", success);
    }


    @Test
    public void test_getRandomAwardId() {
        log.info("测试结果：{} - 奖品ID值", strategyDispatch.getRandomAwardId(100001L));
    }

    // 60:102,103,104,105 200:106,107 1000:105
    @Test
    public void test_getRandomAwardId_ruleWeightValue() {
        log.info("测试结果：{} - 奖品ID值", strategyDispatch.getRandomAwardId(100001L, "60:102,103,104,105"));
        log.info("测试结果：{} - 奖品ID值", strategyDispatch.getRandomAwardId(100001L, "200:106,107"));
        log.info("测试结果：{} - 奖品ID值", strategyDispatch.getRandomAwardId(100001L, "1000:105"));
    }
}
