package com.ytx.ai.workflow.plugin;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.ConditionOptEnum;
import com.ytx.ai.workflow.enums.PluginTypeIdEnum;
import com.ytx.ai.workflow.enums.ValueTypeEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class ConditionPlugin extends BasicPlugin {

    @Getter
    @Setter
    public static class ConditionNodeMeta implements NodeMeta{

        @DependsRef
        private List<LogicBranch> logicBranches;

    }

    @Override
    public String getType() {
        return PluginTypeIdEnum.CONDITION.getType();
    }

    @Override
    public void init() {

    }

    @Override
    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {

        ConditionNodeMeta conditionNodeMeta=(ConditionNodeMeta)flowNode.getMeta();

        List<LogicBranch> logicBranches = conditionNodeMeta.getLogicBranches();
        List<LogicBranch> finalLogicBranches = reorder(logicBranches);

        finalLogicBranches.forEach(group -> {
            ConditionOptEnum groupOpt = ConditionOptEnum.of(group.getOpt());
            if (groupOpt == null) {
                log.error("flow node: " + flowNode.getLabel() + " group: " + group.getId() +
                        " has unknown opt " + group.getOpt());
                group.setResult(false);
            } else if (groupOpt == ConditionOptEnum.OTHERWISE) {
                calOtherwiseGroupResult(group, finalLogicBranches);
            } else {
                // 正常节点，先计算item，再计算group值
                calItemResult(flowNode, group, flowContext);
                calGroupResult(group, groupOpt);
            }
        });

        PluginOutput output = PluginOutput.of();
        finalLogicBranches.forEach(group -> {
            output.addData(group.getId(),
                    Value.builder()
                            .content(group.getResult())
                            .type(ValueTypeEnum.BOOLEAN.getType())
                            .build());
        });

        return output;
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return ConditionNodeMeta.class;
    }


    private void calItemResult(FlowNode flowNode, LogicBranch group, FlowContext flowContext) {
        List<ConditionItem> items = group.getConditions();
        if (ObjectUtil.isEmpty(items)) {
            log.error("flow node: " + flowNode.getLabel() + " condition group: " + group.getId() + " items is empty.");
            group.setResult(false);
            return;
        }

        items.forEach(item -> {
            if (ObjectUtil.isEmpty(item.getOpt())) {
                log.error("flow node: " + flowNode.getLabel() + " condition group: " + group.getId() + " item has empty opt.");
                item.setResult(false);
                return;
            }

            resolveRefValue(item, flowContext);
            ConditionOptEnum opt = ConditionOptEnum.of(item.getOpt());
            switch (opt) {
                case EQUAL:
                    funcEqual(item);
                    break;
                case NOT_EQUAL:
                    break;
                case LEN_GT:
                    break;
                case LEN_GT_OR_EQUAL:
                    funcLenGtOrEqual(item);
                    break;
                case LEN_LT:
                    break;
                case LEN_LT_OR_EQUAL:
                    break;
                case CONTAINS:
                    break;
                case NOT_CONTAIN:
                    break;
                case EMPTY:
                    break;
                case NOT_EMPTY:
                    break;
                case null:
                    break;
                default:
                    log.error("flow node: " + flowNode.getLabel() + " condition group: " + group.getId() +
                            " item has unknown opt " + item.getOpt());
                    item.setResult(false);
                    break;
            }
        });
    }

    private void resolveRefValue(ConditionItem item, FlowContext flowContext) {
        Value left = item.getLeft();
        Value right = item.getRight();
        ValueUtils.resolveRefValue(left, flowContext);
        ValueUtils.resolveRefValue(right, flowContext);
    }
    private void calGroupResult(LogicBranch group, ConditionOptEnum groupOpt) {
        List<ConditionItem> items = group.getConditions();
        AtomicBoolean result = new AtomicBoolean();

        if (ConditionOptEnum.AND == groupOpt) {
            result.set(true);
            items.forEach(item -> {
                result.set(result.get() && item.getResult());
            });
        } else {
            result.set(false);
            items.forEach(item -> {
                result.set(result.get() || item.getResult());
            });
        }

        group.setResult(result.get());
    }

    private void calOtherwiseGroupResult(LogicBranch group, List<LogicBranch> logicBranches) {
        AtomicBoolean hasValidCase = new AtomicBoolean(false);

        logicBranches.stream()
                .filter(item -> {
                    return !ConditionOptEnum.OTHERWISE.getOpt()
                            .equalsIgnoreCase(item.getOpt());
                })
                .forEach(item -> {
                    hasValidCase.set(hasValidCase.get() || item.getResult());
                });

        group.setResult(!hasValidCase.get());
    }

    private List<LogicBranch> reorder(List<LogicBranch> logicBranches) {
        List<LogicBranch> sortedObjects = logicBranches.stream()
                .sorted((g1, g2) -> Integer.compare(g1.getPriority(), g2.getPriority()))
                .collect(Collectors.toList());
        return sortedObjects;
    }

    @Getter
    @Setter
    public static class LogicBranch {
        private String id;
        private String opt=ConditionOptEnum.AND.getOpt();
        private Integer priority;
        private Boolean result;
        @DependsRef
        private List<ConditionItem> conditions;
    }

    @Getter
    @Setter
    public static class ConditionItem {
        private String id;
        @DependsRef
        private Value left;
        private String opt;
        @DependsRef
        private Value right;
        private Boolean result;
    }

    private void funcEqual(ConditionItem item) {
        boolean result = ObjectUtil.equals(
                item.getLeft().getContent(),
                item.getRight().getContent()
        );
        item.setResult(result);
    }

    private void funcLenGtOrEqual(ConditionItem item) {
        int leftLength = 0;
        int rightLength = 0;

        if (ValueTypeEnum.INTEGER.getType()
                .equalsIgnoreCase(item.getLeft().getType())) {
            leftLength = Integer.parseInt(
                    item.getLeft().getContent().toString()
            );
        } else {
            leftLength = ObjectUtil.length(item.getLeft().getContent());
        }

        if (ValueTypeEnum.INTEGER.getType()
                .equalsIgnoreCase(item.getRight().getType())) {
            rightLength = Integer.parseInt(
                    item.getRight().getContent().toString()
            );
        } else {
            rightLength = ObjectUtil.length(item.getRight().getContent());
        }

        boolean result = leftLength >= rightLength;
        item.setResult(result);
    }

}
