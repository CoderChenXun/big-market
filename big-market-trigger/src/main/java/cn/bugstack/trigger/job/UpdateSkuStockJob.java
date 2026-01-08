package cn.bugstack.trigger.job;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuStockService;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.service.IRaffleStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.List;
import java.util.concurrent.ThreadPoolExecutor;

@Slf4j
@Component()
public class UpdateSkuStockJob {

    @Resource
    private IRaffleActivitySkuStockService skuStock;

    @Resource
    private ThreadPoolExecutor executor;

    @Scheduled(cron = "0/5 * * * * ?")
    public void exec() {
        try {
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
        }
    }
}
