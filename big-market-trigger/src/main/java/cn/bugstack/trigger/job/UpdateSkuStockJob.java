package cn.bugstack.trigger.job;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;
import cn.bugstack.domain.activity.service.IRaffleActivitySkuStockService;
import cn.bugstack.domain.strategy.model.valobj.StrategyAwardStockKeyVO;
import cn.bugstack.domain.strategy.service.IRaffleStock;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Slf4j
@Component()
public class UpdateSkuStockJob {

    @Resource
    private IRaffleActivitySkuStockService raffleActivitySkuStockService;

    @Scheduled(cron = "0/5 * * * * ?")
    public void exec() {
        try {
            log.info("定时任务，更新sku消耗库存【延迟队列获取，降低对数据库的更新频次，不要产生竞争】");
            ActivitySkuStockKeyVO activitySkuStockKeyVO = raffleActivitySkuStockService.takeQueueValue();
            if (null == activitySkuStockKeyVO) return;
            log.info("定时任务，更新sku消耗库存 sku:{} activityId:{}", activitySkuStockKeyVO.getSku(), activitySkuStockKeyVO.getActivityId());
            // 更新sku消耗库存
            raffleActivitySkuStockService.updateSkuStock(activitySkuStockKeyVO.getSku());
        } catch (Exception e) {
            log.error("定时任务，更新sku消耗库存失败", e);
        }
    }
}
