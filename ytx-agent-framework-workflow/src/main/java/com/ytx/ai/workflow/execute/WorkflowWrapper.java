package com.ytx.ai.workflow.execute;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.FlowEdge;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.Workflow;
import lombok.Getter;
import lombok.Setter;

import java.util.*;
import java.util.stream.Collectors;


@Getter
@Setter
public class WorkflowWrapper {

    // workflow信息
    private Workflow workflow;

    // 节点ID，节点
    private Map<String, FlowNode> flowNodeMap = new HashMap<>();

    // 边ID，边
    private Map<String, FlowEdge> flowEdgeMap = new HashMap<>();

    // 节点前面的边
    private Map<String, List<FlowEdge>> nodeIncomingEdges = new HashMap<>();

    // 节点后面的边
    private Map<String, List<FlowEdge>> nodeOutgoingEdges = new HashMap<>();

    // 开始节点
    private List<FlowNode> startNodes = new ArrayList<>();

    // 结束节点
    private List<FlowNode> endNodes = new ArrayList<>();

    public WorkflowWrapper(Workflow workflow) {
        this.workflow = workflow;
        this.analyze();
    }

    // 分析整个流程，解析出需要的数据
    private void analyze() {
        analyzeNodes(workflow.getNodes());
        analyzeEdges(workflow.getEdges());
    }

    private void analyzeNodes(List<FlowNode> nodes) {
        if (ObjectUtil.isEmpty(nodes)) {
            return;
        }

        nodes.forEach(node -> {
            flowNodeMap.put(node.getId(), node);

            if (node.isStartNode()) {
                startNodes.add(node);
            }

            if (node.isEndNode()) {
                endNodes.add(node);
            }
        });

        if (ObjectUtil.isEmpty(startNodes)) {
            throw new RuntimeException("Workflow " + workflow.getName() + " missing start node. Fix it before running.");
        } else if (startNodes.size() > 1) {
            throw new RuntimeException("Workflow " + workflow.getName() + " can only have one start node. Fix it before running.");
        }

        if (ObjectUtil.isEmpty(endNodes)) {
            throw new RuntimeException("Workflow " + workflow.getName() + " missing end node. Fix it before running.");
        } else if (endNodes.size() > 1) {
            throw new RuntimeException("Workflow " + workflow.getName() + " can only have one end node. Fix it before running.");
        }
    }

    private void analyzeEdges(List<FlowEdge> edges) {
        if (ObjectUtil.isEmpty(edges)) {
            return;
        }

        edges.forEach(edge -> {
            // 将边ID，边存储到map，方便通过ID获取边
            flowEdgeMap.put(edge.getId(), edge);

            // 处理节点前面的边
            List<FlowEdge> incomingEdges = nodeIncomingEdges.computeIfAbsent(edge.getTarget(), k -> new ArrayList<>());
            incomingEdges.add(edge);

            // 处理节点后面的边
            List<FlowEdge> outgoingEdges = nodeOutgoingEdges.computeIfAbsent(edge.getSource(), k -> new ArrayList<>());
            outgoingEdges.add(edge);
        });
    }

    public FlowNode getStartNode() {
        return startNodes.get(0);
    }

    public FlowNode getEndNode() {
        return endNodes.get(0);
    }

    public FlowNode getNode(String nodeId) {
        return flowNodeMap.get(nodeId);
    }
    // 获取边信息
    public FlowEdge getEdge(String edgeId) {
        return flowEdgeMap.get(edgeId);
    }

    // 获取节点的所有入边
    public List<FlowEdge> getIncomingEdges(String nodeId) {
        return nodeIncomingEdges.get(nodeId);
    }

    // 获取节点的所有出边
    public List<FlowEdge> getOutgoingEdges(String nodeId) {
        return nodeOutgoingEdges.get(nodeId);
    }

    // 获取节点的所有子节点（通过出边）
    public List<FlowNode> getChildNodes(String nodeId) {
        List<FlowEdge> edges = nodeOutgoingEdges.get(nodeId);
        if (ObjectUtil.isEmpty(edges)) {
            return ListUtil.empty();
        }

        return edges.stream()
                .map(edge -> getNode(edge.getTarget()))
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toList());
    }

    // 获取节点的所有父节点（通过入边）
    public List<FlowNode> getParentNodes(String nodeId) {
        List<FlowEdge> edges = nodeIncomingEdges.get(nodeId);
        if (ObjectUtil.isEmpty(edges)) {
            return ListUtil.empty();
        }

        return edges.stream()
                .map(edge -> getNode(edge.getSource()))
                .filter(ObjectUtil::isNotEmpty)
                .collect(Collectors.toList());
    }

    // 计算工作流的深度
    public int calculateDepth() {
        FlowNode root = getStartNode();
        if (root == null) {
            return 0; // 空树深度为 0
        }

        Queue<FlowNode> queue = new LinkedList<>();
        queue.offer(root); // 将根节点加入队列
        int depth = 0;

        while (!queue.isEmpty() && depth <= flowNodeMap.size()) {
            int levelSize = queue.size(); // 当前层的节点数量
            depth++; // 每次进入新的一层，深度加 1

            // 遍历当前层的所有节点
            for (int i = 0; i < levelSize; i++) {
                FlowNode current = queue.poll();
                if (current == null) {
                    continue;
                }

                List<FlowNode> children = getChildNodes(current.getId());
                if (children != null) {
                    queue.addAll(children); // 将子节点加入队列
                }
            }
        }

        return depth;
    }


    public List<List<String>> getAllPaths() {
        FlowNode root = getStartNode();
        List<List<String>> paths = new ArrayList<>();
        if (root == null) {
            return paths;
        }

        // 栈中保存节点以及当前路径
        Stack<Pair<FlowNode, List<String>>> stack = new Stack<>();
        stack.push(new Pair<>(root, new ArrayList<>()));

        while (!stack.isEmpty()) {
            // 弹出栈顶节点及其路径
            Pair<FlowNode, List<String>> pair = stack.pop();
            FlowNode currentNode = pair.node;
            List<String> currentPath = pair.path;

            // 将当前节点加入路径
            currentPath.add(currentNode.getId());

            // 如果是叶子节点，保存路径
            List<FlowNode> children = getChildNodes(currentNode.getId());
            if (children == null || children.isEmpty()) {
                paths.add(currentPath);
            } else {
                // 将子节点及其路径压入栈
                for (FlowNode child : children) {
                    stack.push(new Pair<>(child, new ArrayList<>(currentPath)));
                }
            }
        }

        return paths;
    }

    private static class Pair<T, U> {
        T node;
        U path;

        Pair(T node, U path) {
            this.node = node;
            this.path = path;
        }
    }

}
