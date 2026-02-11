package com.ytx.ai.workflow.node.internal;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.enums.ConditionOptEnum;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.enums.ValueTypeEnum;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import com.ytx.ai.workflow.util.ValueUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Map;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class ConditionNode extends BasicNode {

    @Getter
    @Setter
    public static class ConditionNodeMeta implements NodeMeta{

        @DependsRef
        private List<LogicBranch> logicBranches;

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.CONDITION.getType();
    }

    @Override
    public void init() {

    }

    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {

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

        NodeOutput output = NodeOutput.of();
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
                    funcNotEqual(item);
                    break;
                case LEN_GT:
                    funcLenGt(item);
                    break;
                case LEN_GT_OR_EQUAL:
                    funcLenGtOrEqual(item);
                    break;
                case LEN_LT:
                    funcLenLt(item);
                    break;
                case LEN_LT_OR_EQUAL:
                    funcLenLtOrEqual(item);
                    break;
                case CONTAINS:
                    funcContains(item);
                    break;
                case NOT_CONTAIN:
                    funcNotContains(item);
                    break;
                case EMPTY:
                    funcEmpty(item);
                    break;
                case NOT_EMPTY:
                    funcNotEmpty(item);
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

    /**
     * 判断左右值是否不相等。
     *
     * @param item 条件项
     *             - left: 左值
     *             - right: 右值
     *             - result: 计算结果
     */
    private void funcNotEqual(ConditionItem item) {
        boolean result = !ObjectUtil.equals(
                item.getLeft().getContent(),
                item.getRight().getContent()
        );
        item.setResult(result);
    }

    private void funcLenGtOrEqual(ConditionItem item) {
        int leftLength = resolveComparableLength(item.getLeft());
        int rightLength = resolveComparableLength(item.getRight());

        boolean result = leftLength >= rightLength;
        item.setResult(result);
    }

    /**
     * 判断左值长度是否大于右值长度。
     *
     * @param item 条件项
     *             - left: 参与比较的内容（字符串/数组/集合/Map等），或 INTEGER 类型的数字长度值
     *             - right: 参与比较的内容（字符串/数组/集合/Map等），或 INTEGER 类型的数字长度值
     */
    private void funcLenGt(ConditionItem item) {
        int leftLength = resolveComparableLength(item.getLeft());
        int rightLength = resolveComparableLength(item.getRight());
        item.setResult(leftLength > rightLength);
    }

    /**
     * 判断左值长度是否小于右值长度。
     *
     * @param item 条件项
     *             - left: 参与比较的内容（字符串/数组/集合/Map等），或 INTEGER 类型的数字长度值
     *             - right: 参与比较的内容（字符串/数组/集合/Map等），或 INTEGER 类型的数字长度值
     */
    private void funcLenLt(ConditionItem item) {
        int leftLength = resolveComparableLength(item.getLeft());
        int rightLength = resolveComparableLength(item.getRight());
        item.setResult(leftLength < rightLength);
    }

    /**
     * 判断左值长度是否小于等于右值长度。
     *
     * @param item 条件项
     *             - left: 参与比较的内容（字符串/数组/集合/Map等），或 INTEGER 类型的数字长度值
     *             - right: 参与比较的内容（字符串/数组/集合/Map等），或 INTEGER 类型的数字长度值
     */
    private void funcLenLtOrEqual(ConditionItem item) {
        int leftLength = resolveComparableLength(item.getLeft());
        int rightLength = resolveComparableLength(item.getRight());
        item.setResult(leftLength <= rightLength);
    }

    /**
     * 判断左值是否包含右值。
     * <p>
     * 支持的内容类型：
     * - String：左字符串是否包含右字符串
     * - Collection：是否包含右值（equals 判定）
     * - Array：是否包含右值（equals 判定）
     * - Map：优先判断 key 是否存在；若 key 不存在则判断 value 是否存在
     * </p>
     *
     * @param item 条件项
     *             - left: 容器/字符串
     *             - right: 被包含的值
     */
    private void funcContains(ConditionItem item) {
        boolean result = containsValue(item.getLeft() == null ? null : item.getLeft().getContent(),
                item.getRight() == null ? null : item.getRight().getContent());
        item.setResult(result);
    }

    /**
     * 判断左值是否不包含右值。
     *
     * @param item 条件项
     *             - left: 容器/字符串
     *             - right: 被包含的值
     */
    private void funcNotContains(ConditionItem item) {
        boolean result = !containsValue(item.getLeft() == null ? null : item.getLeft().getContent(),
                item.getRight() == null ? null : item.getRight().getContent());
        item.setResult(result);
    }

    /**
     * 判断左值是否为空（null/空字符串/空集合/空数组/空Map 等）。
     *
     * @param item 条件项
     *             - left: 待判断内容
     */
    private void funcEmpty(ConditionItem item) {
        Object leftContent = item.getLeft() == null ? null : item.getLeft().getContent();
        item.setResult(ObjectUtil.isEmpty(leftContent));
    }

    /**
     * 判断左值是否非空（与 {@link #funcEmpty(ConditionItem)} 相反）。
     *
     * @param item 条件项
     *             - left: 待判断内容
     */
    private void funcNotEmpty(ConditionItem item) {
        Object leftContent = item.getLeft() == null ? null : item.getLeft().getContent();
        item.setResult(ObjectUtil.isNotEmpty(leftContent));
    }

    /**
     * 解析用于 len.* 比较的长度值。
     * <p>
     * 约定：
     * - type == INTEGER：将 content 解析为长度数字（允许字符串数字）
     * - 其它类型：使用 Hutool 的 {@link ObjectUtil#length(Object)} 计算长度（对 null 返回 0）
     * </p>
     *
     * @param value Value 对象（允许为 null）
     * @return 可比较的长度值（不为负数）
     */
    private int resolveComparableLength(Value value) {
        if (value == null) {
            return 0;
        }
        if (ValueTypeEnum.INTEGER.getType().equalsIgnoreCase(value.getType())) {
            Object content = value.getContent();
            if (content == null) {
                return 0;
            }
            String numberStr = content.toString();
            if (StrUtil.isBlank(numberStr)) {
                return 0;
            }
            try {
                return Integer.parseInt(numberStr.trim());
            } catch (NumberFormatException e) {
                // 非法数字时兜底按 0 处理，避免节点执行中断
                return 0;
            }
        }
        return ObjectUtil.length(value.getContent());
    }

    /**
     * 判断 container 是否包含 target。
     *
     * @param container 容器对象（String/Collection/Array/Map）
     * @param target    待匹配对象
     * @return 包含返回 true；否则 false
     */
    private boolean containsValue(Object container, Object target) {
        if (ObjectUtil.isEmpty(container)) {
            return false;
        }

        // 1) 字符串包含：左为字符串时，右值转字符串做子串匹配
        if (container instanceof CharSequence containerStr) {
            String targetStr = target == null ? "" : target.toString();
            return StrUtil.contains(containerStr, targetStr);
        }

        // 2) 集合包含
        if (container instanceof Collection<?> collection) {
            return collection.contains(target);
        }

        // 3) Map 包含：优先 key，其次 value
        if (container instanceof Map<?, ?> map) {
            if (map.containsKey(target)) {
                return true;
            }
            return map.containsValue(target);
        }

        // 4) 数组包含
        if (container.getClass().isArray()) {
            int length = Array.getLength(container);
            for (int i = 0; i < length; i++) {
                Object element = Array.get(container, i);
                if (ObjectUtil.equals(element, target)) {
                    return true;
                }
            }
            return false;
        }

        // 5) 其它类型：无法识别为容器则视为不包含
        return false;
    }

}
