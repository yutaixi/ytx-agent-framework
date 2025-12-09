package com.ytx.ai.workflow.adaptor.processor;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.workflow.FlowEdge;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.Workflow;
import com.ytx.ai.workflow.adaptor.WorkflowAdaptor;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.node.internal.BatchBodyNode;
import com.ytx.ai.workflow.node.internal.BatchNode;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Batch node parse post processor
 * 批量节点解析后处理器
 * 用于处理批量节点和内嵌工作流的解析逻辑
 */
public class BatchNodeParsePostProcessor implements WorkflowParsePostProcessor {

    @Override
    public void process(Workflow workflow) {
        // 如果工作流节点为空，直接返回
        if (ObjectUtil.isEmpty(workflow.getNodes())) {
            return;
        }

        // 过滤出所有 BATCH 类型的节点
        List<FlowNode> batchNodes = workflow.getNodes().stream()
                .filter(node -> WorkflowPluginTypeIdEnum.BATCH.getType().equals(node.getComponentId()))
                .collect(Collectors.toList());

        // 如果没有批量节点，直接返回
        if (ObjectUtil.isEmpty(batchNodes)) {
            return;
        }

        // 用于收集需要移除的节点和边
        List<FlowNode> nodesToRemove = new ArrayList<>();
        List<FlowEdge> edgesToRemove = new ArrayList<>();

        // 遍历每个批量节点
        for (FlowNode batchNode : batchNodes) {
            // 找到连接到当前批量节点的所有边
            List<FlowEdge> connectedEdges = workflow.getEdges().stream()
                    .filter(edge -> edge.getSource().equals(batchNode.getId()))
                    .collect(Collectors.toList());

            // 遍历每条连接的边
            for (FlowEdge edge : connectedEdges) {
                String targetId = edge.getTarget();

                // 找到目标节点
                FlowNode targetNode = workflow.getNodes().stream()
                        .filter(n -> n.getId().equals(targetId))
                        .findFirst()
                        .orElse(null);

                // 如果目标节点是 BATCH_BODY 类型，则处理内嵌工作流
                if (targetNode != null && WorkflowPluginTypeIdEnum.BATCH_BODY.getType().equals(targetNode.getComponentId())) {
                    BatchBodyNode.BatchBodyNodeMeta bodyMeta = (BatchBodyNode.BatchBodyNodeMeta) targetNode.getMeta();
                    Object innerObj = bodyMeta.getInnerWorkflow();

                    // 如果内嵌工作流对象不为空，则解析它
                    if (innerObj != null) {
                        // 将内嵌工作流对象转换为 JSON 对象
                        JSONObject innerJson = JSONUtil.parseObj(JSONUtil.toJsonStr(innerObj));
                        JSONArray iNodes = innerJson.getJSONArray("nodes");
                        JSONArray iEdges = innerJson.getJSONArray("edges");

                        // 递归解析内嵌工作流
                        Workflow innerWorkflow = WorkflowAdaptor.parse(batchNode.getLabel(), null, iNodes, iEdges);

                        // 将解析后的内嵌工作流设置到批量节点的元数据中
                        BatchNode.BatchNodeMeta batchMeta = (BatchNode.BatchNodeMeta) batchNode.getMeta();
                        batchMeta.setWorkflow(innerWorkflow);
                    }

                    // 标记目标节点和边需要移除
                    nodesToRemove.add(targetNode);
                    edgesToRemove.add(edge);
                }
            }
        }

        // 从工作流中移除已处理的节点和边
        workflow.getNodes().removeAll(nodesToRemove);
        workflow.getEdges().removeAll(edgesToRemove);
    }
}
