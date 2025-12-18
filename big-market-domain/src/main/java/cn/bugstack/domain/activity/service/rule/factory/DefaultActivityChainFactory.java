package cn.bugstack.domain.activity.service.rule.factory;

import cn.bugstack.domain.activity.service.rule.IActionChain;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class DefaultActivityChainFactory {
    // 装配活动规则链
    private final IActionChain activityChain;

    public DefaultActivityChainFactory(Map<String, IActionChain> activityRuleChainGroup) {
        // 默认活动规则链
        this.activityChain = activityRuleChainGroup.get(ActionModel.activity_base_action.code);
        activityChain.appendNext(activityRuleChainGroup.get(ActionModel.activity_sku_stock_action.code));
    }

    public IActionChain openActionChain() {
        return activityChain;
    }

    @Getter
    @AllArgsConstructor
    public enum ActionModel {

        activity_base_action("activity_base_action", "活动的库存、时间校验"),
        activity_sku_stock_action("activity_sku_stock_action", "活动sku库存"),
        ;

        private final String code;
        private final String info;

    }
}
