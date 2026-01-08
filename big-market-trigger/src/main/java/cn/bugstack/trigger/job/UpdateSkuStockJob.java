package cn.bugstack.trigger.job;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuStockService;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
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
public class UpdateSkuStockJob {

    @Resource
    private IRaffleActivitySkuStockService skuStock;

    @Resource
    private ThreadPoolExecutor executor;

    @Resource
    private RedissonClient redissonClient;

    @XxlJob("UpdateActivitySkuStockJob")
    public void exec() {
        RLock lock = redissonClient.getLock("big-market-UpdateActivitySkuStockJob");
        boolean isLocked = false;
        try {
            // 为什么加锁？分布式应用N台机器部署互备，任务调度会有N个同时执行，那么这里需要增加抢占机制，谁抢占到谁就执行。完毕后，下一轮继续抢占。
            isLocked = lock.tryLock(3, 0, TimeUnit.SECONDS);
            if(!isLocked){
                return;
            }

            // 查询skuList
            List<Long> skuList = skuStock.querySkuList();
            for(Long sku : skuList){
                executor.execute(() -> {
                    ActivitySkuStockKeyVO activitySkuStockKeyVO = null;
                    try{
                        activitySkuStockKeyVO = skuStock.takeQueueValue(sku);
                    }catch (InterruptedException e){
                        log.error("定时任务，更新活动sku库存失败 sku: {}", sku);
                    }
                    if (null == activitySkuStockKeyVO) return;
                    log.info("定时任务，更新sku消耗库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
                    // 更新sku消耗库存
                    skuStock.updateSkuStock(activitySkuStockKeyVO.getSku());
                });
            }
        } catch (Exception e) {
            log.error("定时任务，更新sku消耗库存失败", e);
        }finally {
            if(isLocked){
                lock.unlock();
            }
        }
    }
}
