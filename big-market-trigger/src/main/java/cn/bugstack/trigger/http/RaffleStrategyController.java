package cn.bugstack.trigger.http;

import cn.bugstack.api.IRaffleStrategyService;
import cn.bugstack.api.dto.RaffleAwardListRequestDTO;
import cn.bugstack.api.dto.RaffleAwardListResponseDTO;
import cn.bugstack.api.dto.RaffleRequestDTO;
import cn.bugstack.api.dto.RaffleResponseDTO;
import cn.bugstack.api.response.Response;
import cn.bugstack.domain.activity.service.IRaffleActivityAccountQuotaService;
import cn.bugstack.domain.strategy.model.entity.RaffleAwardEntity;
import cn.bugstack.domain.strategy.model.entity.RaffleFactorEntity;
import cn.bugstack.domain.strategy.model.entity.StrategyAwardEntity;
import cn.bugstack.domain.strategy.service.IRaffleAward;
import cn.bugstack.domain.strategy.service.IRaffleRule;
import cn.bugstack.domain.strategy.service.IRaffleStrategy;
import cn.bugstack.domain.strategy.service.armory.IStrategyArmory;
import cn.bugstack.types.enums.ResponseCode;
import cn.bugstack.types.exception.AppException;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@CrossOrigin("${app.config.cross-origin}")
@RequestMapping("/api/${app.config.api-version}/raffle/strategy/")
public class RaffleStrategyController implements IRaffleStrategyService {
    /**
     * 策略装配接口
     */
    @Resource
    private IStrategyArmory strategyArmory;

    @Resource
    private IRaffleAward raffleAward;

    @Resource
    private IRaffleRule raffleRule;

    @Resource
    private IRaffleStrategy raffleStrategy;

    @Resource
    private IRaffleActivityAccountQuotaService raffleActivityAccountQuotaService;

    @RequestMapping(value = "strategy_armory", method = RequestMethod.GET)
    @Override
    public Response<Boolean> strategyArmory(@RequestParam Long strategyId) {
        try {
            log.info("策略装配开始 strategyId:{}", strategyId);
            boolean armoryResult = strategyArmory.assembleLotteryStrategy(strategyId);
            Response<Boolean> response = Response.<Boolean>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(armoryResult)
                    .build();
            log.info("策略装配成功 strategyId:{} response:{}", strategyId, JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("策略装配失败 strategyId:{}", strategyId, e);
            return Response.<Boolean>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "query_raffle_award_list", method = RequestMethod.POST)
    @Override
    public Response<List<RaffleAwardListResponseDTO>> queryRaffleAwardList(@RequestBody RaffleAwardListRequestDTO requestDTO) {
        try {
            log.info("查询抽奖奖品列表开始 userId:{} activityId:{}", requestDTO.getUserId(), requestDTO.getActivityId());
            // 1. 进行参数检验
            if (StringUtils.isBlank(requestDTO.getUserId()) || null == requestDTO.getActivityId()) {
                throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
            }
            // 2. 根据活动Id查询策略
            List<StrategyAwardEntity> strategyAwardList = raffleAward.queryStrategyAwardListByActivityId(requestDTO.getActivityId());
            // 3. 获取规则配置
            String[] valueModels = strategyAwardList.stream()
                    .map(strategyAwardEntity -> strategyAwardEntity.getRuleModels())
                    .filter(ruleModels -> null != ruleModels && !ruleModels.isEmpty())
                    .toArray(String[]::new);
            // 4. 查询规则配置
            Map<String, Integer> ruleLockCountMap = raffleRule.queryAwardRuleLockCount(valueModels);
            // 5. 查询当前用户的抽奖次数
            Integer dayPartakeCount = raffleActivityAccountQuotaService.queryRaffleActivityAccountDayPartakeCount(requestDTO.getUserId(), requestDTO.getActivityId());
            // 构建结果
            List<RaffleAwardListResponseDTO> raffleAwardListResponseDTOs = new ArrayList<>(strategyAwardList.size());
            for (StrategyAwardEntity strategyAwardEntity : strategyAwardList) {
                Integer ruleLockCount = ruleLockCountMap.get(strategyAwardEntity.getRuleModels());
                RaffleAwardListResponseDTO raffleAwardListResponseDTO = RaffleAwardListResponseDTO.builder()
                        .awardId(strategyAwardEntity.getAwardId())
                        .awardTitle(strategyAwardEntity.getAwardTitle())
                        .awardSubtitle(strategyAwardEntity.getAwardSubtitle())
                        .sort(strategyAwardEntity.getSort())
                        .awardRuleLockCount(ruleLockCount)
                        .isAwardUnlock(null == ruleLockCount || dayPartakeCount >= ruleLockCount)
                        .waitUnlockCount(null == ruleLockCount ? 0 : dayPartakeCount >= ruleLockCount ? 0 : ruleLockCount - dayPartakeCount)
                        .build();
                raffleAwardListResponseDTOs.add(raffleAwardListResponseDTO);
            }
            Response<List<RaffleAwardListResponseDTO>> response = Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(raffleAwardListResponseDTOs)
                    .build();
            log.info("查询抽奖奖品列表成功 userId:{} response:{}", requestDTO.getUserId(), JSON.toJSONString(response));
            return response;
        } catch (Exception e) {
            log.error("查询抽奖奖品列表失败 userId:{} activityId:{}", requestDTO.getUserId(), requestDTO.getActivityId(),e);
            return Response.<List<RaffleAwardListResponseDTO>>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }

    @RequestMapping(value = "random_raffle", method = RequestMethod.POST)
    @Override
    public Response<RaffleResponseDTO> randomRaffle(@RequestBody RaffleRequestDTO requestDTO) {
        try {
            log.info("随机抽奖开始 strategyId:{}", requestDTO.getStrategyId());
            RaffleAwardEntity raffleAwardEntity = raffleStrategy.performRaffle(RaffleFactorEntity.builder()
                    .strategyId(requestDTO.getStrategyId())
                    .userId("system")
                    .build());
            Response<RaffleResponseDTO> response = Response.<RaffleResponseDTO>builder()
                    .code(ResponseCode.SUCCESS.getCode())
                    .info(ResponseCode.SUCCESS.getInfo())
                    .data(RaffleResponseDTO.builder()
                            .awardId(raffleAwardEntity.getAwardId())
                            .awardIndex(raffleAwardEntity.getSort())
                            .build())
                    .build();
            log.info("随机抽奖成功 strategyId:{} response:{}", requestDTO.getStrategyId(), JSON.toJSONString(response));
            return response;
        } catch (AppException e) {
            log.error("随机抽奖失败 strategyId:{}", requestDTO.getStrategyId(), e);
            return Response.<RaffleResponseDTO>builder()
                    .code(e.getCode())
                    .info(e.getInfo())
                    .build();
        } catch (Exception e) {
            log.error("随机抽奖失败 strategyId:{}", requestDTO.getStrategyId(), e);
            return Response.<RaffleResponseDTO>builder()
                    .code(ResponseCode.UN_ERROR.getCode())
                    .info(ResponseCode.UN_ERROR.getInfo())
                    .build();
        }
    }
}
