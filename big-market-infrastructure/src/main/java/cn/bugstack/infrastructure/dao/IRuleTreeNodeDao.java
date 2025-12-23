package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.RuleTreeNode;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IRuleTreeNodeDao {

    List<RuleTreeNode> queryRuleTreeNodeByTreeId(String treeId);

    List<RuleTreeNode> queryAwardRuleLockCount(String[] treeIds);
}
