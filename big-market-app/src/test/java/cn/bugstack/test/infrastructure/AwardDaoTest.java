package cn.bugstack.test.infrastructure;

import cn.bugstack.infrastructure.dao.IAwardDao;
import cn.bugstack.infrastructure.dao.po.Award;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;
import java.util.List;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class AwardDaoTest {
    @Resource
    private IAwardDao awardDao;

    @Test
    public void test_queryAwardList() {
        List<Award> awardList = awardDao.queryAwardList();
        log.info("测试结果：{}", JSON.toJSONString(awardList));
    }
}
