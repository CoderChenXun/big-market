package cn.bugstack.test.infrastructure;

import cn.bugstack.infrastructure.dao.IRuleTreeDao;
import cn.bugstack.infrastructure.dao.IRuleTreeNodeDao;
import cn.bugstack.infrastructure.dao.IRuleTreeNodeLineDao;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import javax.annotation.Resource;

@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest
public class RuleTreeTest {

    @Resource
    private IRuleTreeNodeDao ruleTreeNodeDao;

    @Resource
    private IRuleTreeNodeLineDao ruleTreeNodeLineDao;

    @Resource
    private IRuleTreeDao ruleTreeDao;

    @Test
    public void test_ruleTree() {
        log.info("测试结果：{}", JSON.toJSONString(ruleTreeDao.queryRuleTreeByTreeId("tree_lock")));
    }

    @Test
    public void test_ruleTreeNode() {
        log.info("测试结果：{}", JSON.toJSONString(ruleTreeNodeDao.queryRuleTreeNodeByTreeId("tree_lock")));
    }

    @Test
    public void test_ruleTreeNodeLine() {
        log.info("测试结果：{}", JSON.toJSONString(ruleTreeNodeLineDao.queryRuleTreeNodeLineByTreeId("tree_lock")));
    }
}
