package cn.bugstack.domain.strategy.service.rule.filter.factory;

import cn.bugstack.domain.strategy.model.entity.RuleActionEntity;
import cn.bugstack.domain.strategy.service.annotation.LogicStrategy;
import cn.bugstack.domain.strategy.service.rule.filter.ILogicFilter;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class DefaultLogicFactory {

    public Map<String, ILogicFilter<?>> logicFilterMap = new ConcurrentHashMap<>();

    public DefaultLogicFactory(List<ILogicFilter<?>> logicFilters) {
        logicFilters.forEach(logic -> {
            LogicStrategy strategy = AnnotationUtils.findAnnotation(logic.getClass(), LogicStrategy.class);
            if (null != strategy) {
                logicFilterMap.put(strategy.logicMode().getCode(), logic);
            }
        });
    }

    public <T extends RuleActionEntity.RaffleEntity> Map<String, ILogicFilter<T>> openLogicFilter() {
        return (Map<String, ILogicFilter<T>>) (Map<?, ?>) logicFilterMap;
    }

    @Getter
    @AllArgsConstructor
    public enum LogicModel {

        RULE_WIGHT("rule_weight", "【抽奖前规则】根据抽奖权重返回可抽奖范围KEY", "before"),
        RULE_BLACKLIST("rule_blacklist", "【抽奖前规则】黑名单规则过滤，命中黑名单则直接返回", "before"),
        RULE_LOCK("rule_lock", "【抽奖中规则】抽奖n次后，可解锁对应奖品", "center"),
        RULE_LUCK_AWARD("rule_luck_award", "【抽奖后规则】兜底策略，当抽中未解锁的奖品或者库存为0的奖品触发", "after"),
        ;

        private final String code;
        private final String info;
        private final String type;

        public static boolean isBefore(String code) {
            // 查询当前过滤规则code对应的枚举类是否是抽奖中需要过滤的
            return "before".equals(LogicModel.valueOf(code.toUpperCase()).getType());
        }

        public static boolean isCenter(String code) {
            // 查询当前过滤规则code对应的枚举类是否是抽奖中需要过滤的
            return "center".equals(LogicModel.valueOf(code.toUpperCase()).getType());
        }

        public static boolean isAfter(String code) {
            // 查询当前过滤规则code对应的枚举类是否是抽奖中需要过滤的
            return "after".equals(LogicModel.valueOf(code.toUpperCase()).getType());
        }
    }

}
