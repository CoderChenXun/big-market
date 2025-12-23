package cn.bugstack.domain.strategy.service;

import java.util.Map;

public interface IRaffleRule {
    /**
     * 根据规则树ID集合查询奖品中加锁数量的配置「部分奖品需要抽奖N次解锁」
     * @param treeIds
     * @return
     */
    Map<String,Integer> queryAwardRuleLockCount(String[] treeIds);
}
