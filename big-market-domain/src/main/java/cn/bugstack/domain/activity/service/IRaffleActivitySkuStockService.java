package cn.bugstack.domain.activity.service;

import cn.bugstack.domain.activity.model.valobj.ActivitySkuStockKeyVO;

import java.util.List;

public interface IRaffleActivitySkuStockService {

    // 从阻塞队列中获取生产者生产的sku任务
    ActivitySkuStockKeyVO takeQueueValue();

    ActivitySkuStockKeyVO takeQueueValue(Long sku) throws InterruptedException;

    // 更新Sku表的库存
    void updateSkuStock(Long sku);

    void clearActivitySkuStock(Long sku);

    void clearQueueValue();

    void clearQueueValue(Long sku);

    List<Long> querySkuList();
}
