package cn.bugstack.trigger.job;

import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleStock;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component()
public class UpdateAwardStockJob {

    @Resource
    private IRaffleStock raffleStock;

    @Resource
    private IRaffleAward raffleAward;

    @Resource
    private ThreadPoolExecutor executor;

    @Resource
    private RedissonClient redissonClient;
    @XxlJob("updateAwardStock")
    public void exec() {
        RLock lock = redissonClient.getLock("big-market-updateAwardStockJob");
        boolean isLocked = false;
        try {
            // 先上锁
            isLocked = lock.tryLock(3, 0, TimeUnit.SECONDS);
            if(!isLocked){
                return;
            }
            // 不同的活动奖品放到不同的阻塞队列中
            List<StrategyAwardStockKeyVO> strategyAwardStockKeyVOList = raffleAward.queryOpenActivityStrategyAwardList();
            if(null == strategyAwardStockKeyVOList){
                return;
            }
            for(StrategyAwardStockKeyVO strategyAwardStockKeyVO : strategyAwardStockKeyVOList){
                executor.execute(() -> {
                    try {
                        StrategyAwardStockKeyVO queueStrategyAwardStockKeyVO = raffleStock.takeQueueValue(strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
                        if(null == queueStrategyAwardStockKeyVO){
                            return;
                        }
                        log.info("定时任务，更新奖品消耗库存 strategyId:{} awardId:{}", queueStrategyAwardStockKeyVO.getStrategyId(), queueStrategyAwardStockKeyVO.getAwardId());
                        raffleStock.updateStrategyAwardStock(queueStrategyAwardStockKeyVO.getStrategyId(), queueStrategyAwardStockKeyVO.getAwardId());
                    } catch (InterruptedException e) {
                        log.error("定时任务，更新奖品消耗库存失败 strategyId:{} awardId:{}", strategyAwardStockKeyVO.getStrategyId(), strategyAwardStockKeyVO.getAwardId());
                    }
                });
            }
        } catch (Exception e) {
            log.error("定时任务，更新奖品消耗库存失败", e);
        }finally {
            if(isLocked){
                lock.unlock();
            }
        }
    }
}
