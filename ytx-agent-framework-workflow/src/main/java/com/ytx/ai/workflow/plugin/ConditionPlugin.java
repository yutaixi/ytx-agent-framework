//package com.ytx.ai.workflow.plugin;
//
//import cn.hutool.core.util.ObjectUtil;
//import cn.hutool.json.JSONUtil;
//import com.ytx.ai.workflow.FlowNode;
//import com.ytx.ai.workflow.NodeMeta;
//import com.ytx.ai.workflow.Value;
//import com.ytx.ai.workflow.enums.ConditionOptEnum;
//import com.ytx.ai.workflow.enums.PluginTypeIdEnum;
//import com.ytx.ai.workflow.enums.ValueTypeEnum;
//import com.ytx.ai.workflow.execute.FlowContext;
//import com.ytx.ai.workflow.util.ValueUtils;
//import lombok.Getter;
//import lombok.Setter;
//import lombok.extern.slf4j.Slf4j;
//
//import java.util.List;
//import java.util.concurrent.atomic.AtomicBoolean;
//import java.util.stream.Collectors;
//
//@Slf4j
//public class ConditionPlugin extends BasicPlugin {
//
//    public static final String CONDITION_GROUP_INFO = "condition_group_info";
//    public static final String CONDITION_GROUP_OTHER_CASE = "condition_group_other_case";
//
//    @Override
//    public String getType() {
//        return PluginTypeIdEnum.CONDITION.getType();
//    }
//
//    @Override
//    public void init() {
//        inputDefinition.add(Param.builder()
//                .name(CONDITION_GROUP_INFO)
//                .type(ValueTypeEnum.OBJECT)
//                .required(true)
//                .build());
//        outputDefinition.add(Param.builder()
//                .name(CONDITION_GROUP_OTHER_CASE)
//                .type(ValueTypeEnum.BOOLEAN)
//                .build());
//    }
//
//    @Override
//    public PluginOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
//        Value groupValue = flowNode.getMetaData().get(CONDITION_GROUP_INFO);
//        if (ObjectUtil.isEmpty(groupValue.getContent())) {
//            log.error("flow node: " + flowNode.getLabel() + " condition info is empty.");
//            return null;
//        }
//
//        List<ConditionGroup> conditionGroups = JSONUtil.toList(
//                JSONUtil.toJsonStr(groupValue.getContent()),
//                ConditionGroup.class
//        );
//        List<ConditionGroup> finalConditionGroups = reorder(conditionGroups);
//
//        finalConditionGroups.forEach(group -> {
//            ConditionOptEnum groupOpt = ConditionOptEnum.of(group.getOpt());
//            if (groupOpt == null) {
//                log.error("flow node: " + flowNode.getLabel() + " group: " + group.getId() +
//                        " has unknown opt " + group.getOpt());
//                group.setResult(false);
//            } else if (groupOpt == ConditionOptEnum.OTHERWISE) {
//                calOtherwiseGroupResult(group, finalConditionGroups);
//            } else {
//                // 正常节点，先计算item，再计算group值
//                calItemResult(flowNode, group, flowContext);
//                calGroupResult(group, groupOpt);
//            }
//        });
//
//        PluginOutput output = PluginOutput.of();
//        finalConditionGroups.forEach(group -> {
//            output.addData(group.getId(),
//                    Value.builder()
//                            .content(group.getResult())
//                            .type(ValueTypeEnum.BOOLEAN.getType())
//                            .build());
//        });
//
//        return output;
//    }
//
//    @Override
//    public Class<? extends NodeMeta> getMetaClass() {
//        return null;
//    }
//
//
//    private void calItemResult(FlowNode flowNode, ConditionGroup group, FlowContext flowContext) {
//        List<ConditionItem> items = group.getItems();
//        if (ObjectUtil.isEmpty(items)) {
//            log.error("flow node: " + flowNode.getLabel() + " condition group: " + group.getId() + " items is empty.");
//            group.setResult(false);
//            return;
//        }
//
//        items.forEach(item -> {
//            if (ObjectUtil.isEmpty(item.getOpt())) {
//                log.error("flow node: " + flowNode.getLabel() + " condition group: " + group.getId() + " item has empty opt.");
//                item.setResult(false);
//                return;
//            }
//
//            resolveRefValue(item, flowContext);
//            ConditionOptEnum opt = ConditionOptEnum.of(item.getOpt());
//            switch (opt) {
//                case EQUAL:
//                    funcEqual(item);
//                    break;
//                case NOT_EQUAL:
//                    break;
//                case LEN_GT:
//                    break;
//                case LEN_GT_OR_EQUAL:
//                    funcLenGtOrEqual(item);
//                    break;
//                case LEN_LT:
//                    break;
//                case LEN_LT_OR_EQUAL:
//                    break;
//                case CONTAINS:
//                    break;
//                case NOT_CONTAIN:
//                    break;
//                case EMPTY:
//                    break;
//                case NOT_EMPTY:
//                    break;
//                case null:
//                    break;
//                default:
//                    log.error("flow node: " + flowNode.getLabel() + " condition group: " + group.getId() +
//                            " item has unknown opt " + item.getOpt());
//                    item.setResult(false);
//                    break;
//            }
//        });
//    }
//
//    private void resolveRefValue(ConditionItem item, FlowContext flowContext) {
//        Value left = item.getLeft();
//        Value right = item.getRight();
//        ValueUtils.resolveRefValue(left, flowContext);
//        ValueUtils.resolveRefValue(right, flowContext);
//    }
//    private void calGroupResult(ConditionGroup group, ConditionOptEnum groupOpt) {
//        List<ConditionItem> items = group.getItems();
//        AtomicBoolean result = new AtomicBoolean();
//
//        if (ConditionOptEnum.AND == groupOpt) {
//            result.set(true);
//            items.forEach(item -> {
//                result.set(result.get() && item.getResult());
//            });
//        } else {
//            result.set(false);
//            items.forEach(item -> {
//                result.set(result.get() || item.getResult());
//            });
//        }
//
//        group.setResult(result.get());
//    }
//
//    private void calOtherwiseGroupResult(ConditionGroup group, List<ConditionGroup> conditionGroups) {
//        AtomicBoolean hasValidCase = new AtomicBoolean(false);
//
//        conditionGroups.stream()
//                .filter(item -> {
//                    return !ConditionOptEnum.OTHERWISE.getOpt()
//                            .equalsIgnoreCase(item.getOpt());
//                })
//                .forEach(item -> {
//                    hasValidCase.set(hasValidCase.get() || item.getResult());
//                });
//
//        group.setResult(!hasValidCase.get());
//    }
//
//    private List<ConditionGroup> reorder(List<ConditionGroup> conditionGroups) {
//        List<ConditionGroup> sortedObjects = conditionGroups.stream()
//                .sorted((g1, g2) -> Integer.compare(g1.getOrder(), g2.getOrder()))
//                .collect(Collectors.toList());
//        return sortedObjects;
//    }
//
//    @Getter
//    @Setter
//    public static class ConditionGroup {
//        private String id;
//        private String opt;
//        private Integer order;
//        private Boolean result;
//        private List<ConditionItem> items;
//    }
//
//    @Getter
//    @Setter
//    public static class ConditionItem {
//        private Value left;
//        private String opt;
//        private Value right;
//        private Boolean result;
//    }
//
//    private void funcEqual(ConditionItem item) {
//        boolean result = ObjectUtil.equals(
//                item.getLeft().getContent(),
//                item.getRight().getContent()
//        );
//        item.setResult(result);
//    }
//
//    private void funcLenGtOrEqual(ConditionItem item) {
//        int leftLength = 0;
//        int rightLength = 0;
//
//        if (ValueTypeEnum.INTEGER.getType()
//                .equalsIgnoreCase(item.getLeft().getType())) {
//            leftLength = Integer.parseInt(
//                    item.getLeft().getContent().toString()
//            );
//        } else {
//            leftLength = ObjectUtil.length(item.getLeft().getContent());
//        }
//
//        if (ValueTypeEnum.INTEGER.getType()
//                .equalsIgnoreCase(item.getRight().getType())) {
//            rightLength = Integer.parseInt(
//                    item.getRight().getContent().toString()
//            );
//        } else {
//            rightLength = ObjectUtil.length(item.getRight().getContent());
//        }
//
//        boolean result = leftLength >= rightLength;
//        item.setResult(result);
//    }
//
//}
