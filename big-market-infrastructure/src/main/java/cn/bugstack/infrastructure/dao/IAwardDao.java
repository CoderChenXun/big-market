package cn.bugstack.infrastructure.dao;

import cn.bugstack.infrastructure.dao.po.Award;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface IAwardDao {

    List<Award> queryAwardList();

    String queryAwardConfig(Integer awardId);

    String queryAwardKey(Integer awardId);
}
