package cn.bugstack.domain.strategy.model.valobj;

import cn.bugstack.domain.strategy.service.rule.filter.factory.DefaultLogicFactory;
import cn.bugstack.types.common.Constants;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StrategyAwardRuleModelVO {
    private String ruleModels;

    public String[] raffleCenterRuleModelList() {
        List<String> ruleModelList = new ArrayList<>();
        String[] splitRuleModels = ruleModels.split(Constants.SPLIT);
        for (String splitRuleModel : splitRuleModels) {
            if (DefaultLogicFactory.LogicModel.isCenter(splitRuleModel)) {
                ruleModelList.add(splitRuleModel);
            }
        }
        return ruleModelList.toArray(new String[0]);
    }
}
