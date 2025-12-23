package cn.bugstack.domain.activity.service.armory;

import cn.bugstack.domain.activity.model.entity.ActivitySkuEntity;
import cn.bugstack.domain.activity.repository.IActivityRepository;
import cn.bugstack.types.common.Constants;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class ActivityArmoryDispatch implements IActivityArmory, IActivityDispatch{

    @Resource
    private IActivityRepository activityRepository;

    @Override
    public boolean assembleActivitySku(Long sku) {
        // 根据sku 获取活动信息
        ActivitySkuEntity activitySkuEntity = activityRepository.queryActivitySku(sku);

        // 将sku的库存信息缓存到redis中
        cacheActivitySkuStockCount(sku, activitySkuEntity.getStockCount());

        // 预热活动【查询时预热到缓存】
        activityRepository.queryRaffleActivityByActivityId(activitySkuEntity.getActivityId());

        // 预热活动次数(活动次数是 sku对应添加的使用次数信息)【查询时预热到缓存】
        activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());

        return true;
    }

    @Override
    public Boolean assembleActivitySkuByActivityId(Long activityId) {
        // 首先根据活动ID查询活动SKU信息
        List<ActivitySkuEntity> activitySkuEntityList = activityRepository.queryActivitySkuByActivityId(activityId);
        if (null == activitySkuEntityList) {
            throw new AppException(ResponseCode.ACTIVITY_SKU_LIST_NOT_EXIST.getCode(), ResponseCode.ACTIVITY_SKU_LIST_NOT_EXIST.getInfo());
        }
        // 缓存活动SKU信息
        for (ActivitySkuEntity activitySkuEntity : activitySkuEntityList) {
            // 缓存活动sku库存
            cacheActivitySkuStockCount(activitySkuEntity.getSku(), activitySkuEntity.getStockCount());
            // 缓存活动sku对应活动次数
            activityRepository.queryRaffleActivityCountByActivityCountId(activitySkuEntity.getActivityCountId());
        }

        // 预热活动【查询时预热到缓存】
        activityRepository.queryRaffleActivityByActivityId(activityId);
        return true;
    }

    private void cacheActivitySkuStockCount(Long sku, Integer stockCount) {
        String cachedKey = Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
        activityRepository.cacheActivitySkuStockCount(cachedKey, stockCount);
    }

    /**
     * 扣减奖品缓存库存
     * @param sku 互动SKU
     * @param endDateTime 活动结束时间，根据结束时间设置加锁的key为结束时间
     * @return
     */
    @Override
    public boolean subtractionActivitySkuStock(Long sku, Date endDateTime) {
        String cachedKey = Constants.RedisKey.ACTIVITY_SKU_STOCK_COUNT_KEY + sku;
        return activityRepository.subtractionActivitySkuStock(sku,cachedKey, endDateTime);
    }
}
