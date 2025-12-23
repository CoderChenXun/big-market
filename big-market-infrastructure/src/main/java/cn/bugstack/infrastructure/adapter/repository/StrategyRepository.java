package cn.bugstack.infrastructure.adapter.repository;

import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyRuleEntity;
import cn.bugstack.domain.strategy.model.valobj.*;
import cn.bugstack.domain.strategy.repository.IStrategyRepository;
import cn.bugstack.infrastructure.dao.*;
import cn.bugstack.infrastructure.dao.po.*;
import cn.bugstack.infrastructure.redis.IRedisService;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

import static cn.bugstack.types.enums.ResponseCode.UN_ASSEMBLED_STRATEGY_ARMORY;

@Slf4j
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

    @Resource
    private IRuleTreeDao ruleTreeDao;

    @Resource
    private IRuleTreeNodeDao ruleTreeNodeDao;

    @Resource
    private IRuleTreeNodeLineDao ruleTreeNodeLineDao;

    @Resource
    private IRaffleActivityDao raffleActivityDao;

    /**
     * 通过策略id查询策略奖品
     *
     * @param strategyId
     * @return
     */
    @Override
    public List<StrategyAwardEntity> queryStrategyAwardListByStrategyId(Long strategyId) {
        // 1. 首先从redis中查
        String cacheKey = Constants.RedisKey.STRATEGY_AWARD_LIST_KEY + strategyId;
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
                    .awardTitle(strategyAward.getAwardTitle())
                    .awardSubtitle(strategyAward.getAwardSubtitle())
                    .sort(strategyAward.getSort())
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
        String cachedKey = Constants.RedisKey.STRATEGY_RATE_RANGE_KEY + key;
        if(!redisService.isExists(cachedKey)){
            throw new AppException(UN_ASSEMBLED_STRATEGY_ARMORY.getCode(),cachedKey + Constants.COLON + UN_ASSEMBLED_STRATEGY_ARMORY.getInfo());
        }
        // 在缓存中获取抽奖概率查找表的大小
        return redisService.getValue(cachedKey);
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

    @Override
    public RuleTreeVO queryRuleTreeVo(String treeId) {
        // 优先从缓存获取
        String cacheKey = Constants.RedisKey.RULE_TREE_VO_KEY + treeId;
        RuleTreeVO ruleTreeVOCache = redisService.getValue(cacheKey);
        if (null != ruleTreeVOCache) return ruleTreeVOCache;

        // 1. 查询出treeId对应的树
        RuleTree ruleTree = ruleTreeDao.queryRuleTreeByTreeId(treeId);
        List<RuleTreeNode> ruleTreeNodes = ruleTreeNodeDao.queryRuleTreeNodeByTreeId(treeId);
        List<RuleTreeNodeLine> ruleTreeNodeLines = ruleTreeNodeLineDao.queryRuleTreeNodeLineByTreeId(treeId);

        // 2.1 组装出formNode ——》 List<RuleTreeNodeLine>的Map
        Map<String, List<RuleTreeNodeLineVO>> ruleTreeNodeLineMap = new HashMap<>();
        for (RuleTreeNodeLine ruleTreeNodeLine : ruleTreeNodeLines) {
            // 转换为RuleTreeNodeLineVO
            RuleTreeNodeLineVO ruleTreeNodeLineVO = RuleTreeNodeLineVO.builder()
                    .treeId(ruleTreeNodeLine.getTreeId())
                    .ruleNodeFrom(ruleTreeNodeLine.getRuleNodeFrom())
                    .ruleNodeTo(ruleTreeNodeLine.getRuleNodeTo())
                    .ruleLimitType(RuleLimitTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitType()))
                    .ruleLimitValue(RuleLogicCheckTypeVO.valueOf(ruleTreeNodeLine.getRuleLimitValue()))
                    .build();
            List<RuleTreeNodeLineVO> ruleTreeNodeLineVOS = ruleTreeNodeLineMap.computeIfAbsent(ruleTreeNodeLine.getRuleNodeFrom(), k -> new ArrayList<>());
            ruleTreeNodeLineVOS.add(ruleTreeNodeLineVO);
        }

        // tree node转换为Map结构
        Map<String, RuleTreeNodeVO> treeNodeMap = new HashMap<>();
        for (RuleTreeNode ruleTreeNode : ruleTreeNodes) {
            // 转换为RuleTreeNodeVO
            RuleTreeNodeVO ruleTreeNodeVO = RuleTreeNodeVO.builder()
                    .treeNodeLineVOList(ruleTreeNodeLineMap.get(ruleTreeNode.getRuleKey()))
                    .treeId(ruleTreeNode.getTreeId())
                    .ruleDesc(ruleTreeNode.getRuleDesc())
                    .ruleValue(ruleTreeNode.getRuleValue())
                    .ruleKey(ruleTreeNode.getRuleKey())
                    .build();
            treeNodeMap.putIfAbsent(ruleTreeNode.getRuleKey(), ruleTreeNodeVO);
        }

        // 将ruleTree转换为Vo
        RuleTreeVO ruleTreeVO = RuleTreeVO.builder()
                .treeId(ruleTree.getTreeId())
                .treeName(ruleTree.getTreeName())
                .treeDesc(ruleTree.getTreeDesc())
                .treeRootRuleNode(ruleTree.getTreeNodeRuleKey())
                .treeNodeMap(treeNodeMap)
                .build();

        redisService.setValue(cacheKey, ruleTreeVO);
        return ruleTreeVO;
    }

    @Override
    public void cacheStrategyAwardCount(Long strategyId, Integer awardId, Integer awardCount) {
        String cachedKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_KEY + strategyId + Constants.UNDERLINE + awardId;
        if (redisService.isExists(cachedKey)) {
            return;
        }
        redisService.setAtomicLong(cachedKey, awardCount);
    }

    @Override
    public Boolean subtractionAwardStock(String cachedKey) {
        // 1. 首先检查redis缓存是否有该key
        // 1.1 没有则返回false
        if (!redisService.isExists(cachedKey)) {
            return false;
        }
        // 1.2 存在则减库存
        long surPlus = redisService.decr(cachedKey);
        if (surPlus < 0) {
            // 库存为0个，恢复为0
            redisService.setValue(cachedKey, 0);
            return false;
        }

        // 2. 锁住当前库存
        String lockedKey = cachedKey + Constants.UNDERLINE + surPlus;
        Boolean setNx = redisService.setNx(lockedKey);
        if (!setNx) {
            log.info("策略奖品库存加锁失败 {}", lockedKey);
        }
        return setNx;
    }

    @Override
    public void awardStockConsumeSendQueue(StrategyAwardStockKeyVO strategyAwardStockKeyVO) {
        String cachedKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUERY_KEY;
        // 将扣减库存时间存入到阻塞队列中
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redisService.getBlockingQueue(cachedKey);
        RDelayedQueue<StrategyAwardStockKeyVO> delayedQueue = redisService.getDelayedQueue(blockingQueue);
        delayedQueue.offer(strategyAwardStockKeyVO, 3, TimeUnit.SECONDS);
    }

    @Override
    public StrategyAwardStockKeyVO takeQueueValue() {
        // 1. 组装缓存key
        String cachedKey = Constants.RedisKey.STRATEGY_AWARD_COUNT_QUERY_KEY;
        RBlockingQueue<StrategyAwardStockKeyVO> blockingQueue = redisService.getBlockingQueue(cachedKey);
        return blockingQueue.poll();
    }

    @Override
    public void updateStrategyAwardStock(Long strategyId, Integer awardId) {
        // 1. 根据strategyId和awardId更新奖品库存
        StrategyAward strategyAwardReq = new StrategyAward();
        strategyAwardReq.setStrategyId(strategyId);
        strategyAwardReq.setAwardId(awardId);
        strategyAwardDao.updateStrategyAwardStock(strategyAwardReq);
    }

    @Override
    public StrategyAwardEntity queryStrategyAwardEntity(Long strategyId, Integer awardId) {
        // 1. 优先从缓存中读取
        String cachedKey = Constants.RedisKey.STRATEGY_AWARD_KEY + strategyId + Constants.UNDERLINE + awardId;
        StrategyAwardEntity strategyAwardEntity = redisService.getValue(cachedKey);
        if (null != strategyAwardEntity) return strategyAwardEntity;

        // 2. 从数据库中读取
        StrategyAward strategyAwardReq = new StrategyAward();
        strategyAwardReq.setStrategyId(strategyId);
        strategyAwardReq.setAwardId(awardId);
        StrategyAward strategyAwardRes = strategyAwardDao.queryStrategyAwardEntity(strategyAwardReq);
        // 2.1 将实体类对象与Entity对象进行转换
        strategyAwardEntity = StrategyAwardEntity.builder()
                .strategyId(strategyAwardRes.getStrategyId())
                .awardId(strategyAwardRes.getAwardId())
                .awardCount(strategyAwardRes.getAwardCount())
                .awardCountSurplus(strategyAwardRes.getAwardCountSurplus())
                .awardRate(strategyAwardRes.getAwardRate())
                .awardTitle(strategyAwardRes.getAwardTitle())
                .awardSubtitle(strategyAwardRes.getAwardSubtitle())
                .sort(strategyAwardRes.getSort())
                .build();

        // 3. 保存Entity对象到缓存中
        redisService.setValue(cachedKey, strategyAwardEntity);
        return strategyAwardEntity;
    }

    @Override
    public Long queryStrategyIdByActivityId(Long activityId) {
        RaffleActivity raffleActivityReq = new RaffleActivity();
        raffleActivityReq.setActivityId(activityId);
        RaffleActivity raffleActivityRes = raffleActivityDao.queryStrategyIdByActivityId(raffleActivityReq);
        return raffleActivityRes.getStrategyId();
    }
}
