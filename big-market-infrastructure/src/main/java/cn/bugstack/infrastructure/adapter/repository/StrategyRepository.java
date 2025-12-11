package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardRuleModelVO;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.infrastructure.dao.IStrategyAwardDao;
import cn.bugstack.infrastructure.dao.IStrategyDao;
import cn.bugstack.infrastructure.dao.IStrategyRuleDao;
import cn.bugstack.infrastructure.dao.po.Strategy;
import cn.bugstack.infrastructure.dao.po.StrategyAward;
import cn.bugstack.infrastructure.dao.po.StrategyRule;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import com.alibaba.fastjson.JSON;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Repository
public class StrategyRepository implements IStrategyRepository {

    @Resource
    private IStrategyAwardDao strategyAwardDao;

    @Resource
    private IRedisService redisService;

    @Resource
    private IStrategyDao strategyDao;

    @Resource
    private IStrategyRuleDao strategyRuleDao;

    /**
     * 通过策略id查询策略奖品
     *
     * @param strategyId
     * @return
     */
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardListByStrategyId(Long strategyId) {
        // 1. 首先从redis中查
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId;
        List<StrategyAwardEntity> strategyAwardEntityList = redisService.getValue(cacheKey);
        if (null != strategyAwardEntityList && !strategyAwardEntityList.isEmpty()) {
            return strategyAwardEntityList;
        }
        // 2. redis中查不到则在数据库中查
        List<StrategyAward> strategyAwardList = strategyAwardDao.queryStrategyAwardListByStrategyId(strategyId);
        if (null == strategyAwardList || strategyAwardList.isEmpty()) {
            // 3. 数据库中查不到则返回空，则保存空列表到redis中
            strategyAwardEntityList = Collections.emptyList();
            redisService.setValue(cacheKey, strategyAwardEntityList);
            return strategyAwardEntityList;
        }
        // 3. 查询到策略奖品
        strategyAwardEntityList = new ArrayList<>(strategyAwardList.size());
        for (StrategyAward strategyAward : strategyAwardList) {
            StrategyAwardEntity strategyAwardEntity = StrategyAwardEntity.builder()
                    .strategyId(strategyAward.getStrategyId())
                    .awardId(strategyAward.getAwardId())
                    .awardCount(strategyAward.getAwardCount())
                    .awardCountSurplus(strategyAward.getAwardCountSurplus())
                    .awardRate(strategyAward.getAwardRate())
                    .build();
            strategyAwardEntityList.add(strategyAwardEntity);
        }
        // 3.1 加入到redis缓存中
        redisService.setValue(cacheKey, strategyAwardEntityList);
        return strategyAwardEntityList;
    }

    @Override
    public void storeStrategyAwardSearchRateTables(String key, Integer tableSize, Map<Integer, Integer> shuffleStrategyAwardSearchRateTables) {
        // 1. 首先保存tableSize到redis中
        String cachedTableSizeKey = Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key;
        redisService.setValue(cachedTableSizeKey, tableSize);
        // 2. 保存概率表
        String cachedRateTableKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key;
        Map<Integer, Integer> cacheTableMap = redisService.getMap(cachedRateTableKey);
        cacheTableMap.putAll(shuffleStrategyAwardSearchRateTables);
    }

    @Override
    public int getRateRange(String key) {
        String cachedTableSizeKey = Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key;
        // 在缓存中获取抽奖概率查找表的大小
        return redisService.getValue(cachedTableSizeKey);
    }

    @Override
    public Integer getStrategyAwardAssemble(String key, int nextInt) {
        String cachedRateTableKey = Constants.RedisKey.STRATEGY_RATE_TABLE_KEY + key;
        return redisService.getFromMap(cachedRateTableKey, nextInt);
    }

    /**
     * 查询策略实体
     *
     * @param strategyId
     * @return
     */
    @Override
    public StrategyEntity queryStrategyEntityByStrategyId(Long strategyId) {
        // 1. 优先从缓存中获取
        String cachedKey = Constants.RedisKey.STRATEGY_KEY + strategyId;
        StrategyEntity strategyEntity = redisService.getValue(cachedKey);
        if (null != strategyEntity) {
            return strategyEntity;
        }
        // 2. 从数据库中获取
        Strategy strategy = strategyDao.queryStrategyEntityByStrategyId(strategyId);
        strategyEntity = StrategyEntity.builder()
                .strategyId(strategy.getStrategyId())
                .strategyDesc(strategy.getStrategyDesc())
                .ruleModels(strategy.getRuleModels())
                .build();
        redisService.setValue(cachedKey, strategyEntity);
        return strategyEntity;
    }

    @Override
    public StrategyRuleEntity queryStrategyRule(Long strategyId, String ruleModel) {
        StrategyRule strategyRuleReq = new StrategyRule();
        strategyRuleReq.setStrategyId(strategyId);
        strategyRuleReq.setRuleModel(ruleModel);
        StrategyRule strategyRuleRes = strategyRuleDao.queryStrategyRule(strategyRuleReq);
        return StrategyRuleEntity.builder()
                .strategyId(strategyRuleRes.getStrategyId())
                .awardId(strategyRuleRes.getAwardId())
                .ruleType(strategyRuleRes.getRuleType())
                .ruleModel(strategyRuleRes.getRuleModel())
                .ruleValue(strategyRuleRes.getRuleValue())
                .ruleDesc(strategyRuleRes.getRuleDesc())
                .build();
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, Integer awardId, String ruleModel) {
        StrategyRule strategyRuleReq = new StrategyRule();
        strategyRuleReq.setStrategyId(strategyId);
        strategyRuleReq.setAwardId(awardId);
        strategyRuleReq.setRuleModel(ruleModel);
        return strategyRuleDao.queryStrategyRuleValue(strategyRuleReq);
    }

    @Override
    public String queryStrategyRuleValue(Long strategyId, String ruleModel) {
        return queryStrategyRuleValue(strategyId, null, ruleModel);
    }

    @Override
    public StrategyAwardRuleModelVO queryStrategyAwardRuleModels(Long strategyId, Integer awardId) {
        // 1. 组装查询条件
        StrategyAward strategyAwardReq = new StrategyAward();
        strategyAwardReq.setStrategyId(strategyId);
        strategyAwardReq.setAwardId(awardId);
        StrategyAward strategyAwardRes = strategyAwardDao.queryStrategyAwardRuleModels(strategyAwardReq);
        return StrategyAwardRuleModelVO.builder()
                .ruleModels(strategyAwardRes.getRuleModels())
                .build();
    }
}
