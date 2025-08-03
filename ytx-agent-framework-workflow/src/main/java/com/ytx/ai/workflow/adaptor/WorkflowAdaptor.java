package com.ytx.ai.workflow.adaptor;


import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.entity.SkillEntity;
import com.ytx.ai.base.workflow.Flow;
import com.ytx.ai.workflow.*;
import com.ytx.ai.workflow.enums.ComponentTypeEnum;
import com.ytx.ai.workflow.plugin.WorkflowPlugin;
import com.ytx.ai.workflow.register.WorkflowPluginRegister;

import java.util.ArrayList;
import java.util.List;

public class WorkflowAdaptor {

    public static Workflow parse(Flow flow){

        Workflow workflow=Workflow.builder().id(flow.getId()).name(flow.getName()).description(flow.getDescription()).build();
        String definition=flow.getDefinition();
        if(ObjectUtil.isEmpty(definition)){
            return workflow;
        }
        JSONObject workflowObj= JSONUtil.parseObj(definition);
        JSONArray nodes=workflowObj.getJSONArray("nodes");
        JSONArray edges=workflowObj.getJSONArray("edges");

        List<FlowNode> flowNodes= parseNodes(nodes);
        List<FlowEdge> flowEdges= parseEdges(edges);
        workflow.setNodes(flowNodes);
        workflow.setEdges(flowEdges);
        return workflow;
    }


    private static List<FlowNode> parseNodes(JSONArray nodes){
        List<FlowNode> flowNodes=new ArrayList<>();
        if(ObjectUtil.isEmpty(nodes)){
            return flowNodes;
        }
        for(int i=0;i<nodes.size();i++){
            JSONObject nodeData=nodes.getJSONObject(i).getJSONObject("data");
            FlowNode flowNode=FlowNode.builder()
                    .id(nodeData.getStr("id"))
                    .label(nodeData.getStr("label"))
                    .componentType(ComponentTypeEnum.plugin)
                    .componentId(nodeData.getStr("type"))
                    .build();
            WorkflowPlugin workflowPlugin = WorkflowPluginRegister.get(flowNode.getComponentId());
            NodeMeta nodeMeta=JSONUtil.toBean(nodeData, workflowPlugin.getMetaClass());
            flowNode.setMeta(nodeMeta);
            flowNodes.add(flowNode);
        }
        return flowNodes;
    }

    private static List<FlowEdge> parseEdges(JSONArray edges){
        List<FlowEdge> flowEdges=new ArrayList<>();
        if(ObjectUtil.isEmpty(edges)){
            return flowEdges;
        }
        edges.forEach(item->{
            FlowEdge edge=JSONUtil.toBean(item.toString(),FlowEdge.class);
            flowEdges.add(edge);
        });
        return flowEdges;
    }

    public static void main(String[] args) {
        SkillEntity skill=new SkillEntity();
        String definition="{\"nodes\":[{\"id\":\"start-1751552122417_ps8sv3\",\"type\":\"CustomNode\",\"position\":{\"x\":96.82097090517982,\"y\":395.00049455981815},\"data\":{\"id\":\"start-1751552122417_ps8sv3\",\"label\":\"start Node\",\"type\":\"start\",\"inputs\":{\"variables\":[{\"id\":\"1751552257395_rmuj80\",\"name\":\"question\",\"type\":\"string\",\"description\":\"问题\"}]}},\"width\":200,\"height\":89,\"selected\":true,\"positionAbsolute\":{\"x\":96.82097090517982,\"y\":395.00049455981815},\"dragging\":false},{\"id\":\"end-1751552124201_b9r5dh\",\"type\":\"CustomNode\",\"position\":{\"x\":718.7897338177313,\"y\":426.96487039468775},\"data\":{\"id\":\"end-1751552124201_b9r5dh\",\"label\":\"end Node\",\"type\":\"end\",\"outputs\":{\"variables\":[{\"id\":\"1751552280803_3rwxs0\",\"name\":\"output\",\"type\":\"string\",\"source\":{\"type\":\"ref\",\"nId\":\"llm-1751552125176_d6h00l\",\"vName\":\"answer\",\"vGroup\":\"outputs\"}}],\"outputText\":\"\"}},\"width\":200,\"height\":89,\"selected\":false,\"positionAbsolute\":{\"x\":718.7897338177313,\"y\":426.96487039468775},\"dragging\":false},{\"id\":\"llm-1751552125176_d6h00l\",\"type\":\"CustomNode\",\"position\":{\"x\":412.4797713324439,\"y\":527.2995927233142},\"data\":{\"id\":\"llm-1751552125176_d6h00l\",\"label\":\"llm Node\",\"type\":\"llm\",\"inputs\":{\"variables\":[{\"id\":\"1751552131684_zu7zhp\",\"name\":\"aa\",\"type\":\"string\"},{\"id\":\"1751552131945_jtiadf\",\"name\":\"bb\",\"type\":\"string\"}]},\"metaData\":{\"modelCode\":{\"name\":\"modelCode\",\"content\":\"gpt-4o\"},\"systemPrompt\":{\"name\":\"systemPrompt\",\"content\":\"你是一个机器人\"},\"userPrompt\":{\"name\":\"userPrompt\",\"content\":\"不应该是这样的的\"}},\"outputs\":{\"variables\":[{\"id\":\"1751552247228_ccabut\",\"name\":\"answer\",\"type\":\"string\",\"description\":\"水电费水电费\"}]}},\"width\":216,\"height\":89,\"selected\":false,\"positionAbsolute\":{\"x\":412.4797713324439,\"y\":527.2995927233142},\"dragging\":false}],\"edges\":[{\"source\":\"start-1751552122417_ps8sv3\",\"sourceHandle\":\"start-out\",\"target\":\"llm-1751552125176_d6h00l\",\"targetHandle\":\"llm-in\",\"id\":\"reactflow__edge-start-1751552122417_ps8sv3start-out-llm-1751552125176_d6h00lllm-in\"},{\"source\":\"llm-1751552125176_d6h00l\",\"sourceHandle\":\"llm-out\",\"target\":\"end-1751552124201_b9r5dh\",\"targetHandle\":\"end-out\",\"id\":\"reactflow__edge-llm-1751552125176_d6h00lllm-out-end-1751552124201_b9r5dhend-out\"}]}";
        skill.setDefinition(definition);
        Workflow flow=WorkflowAdaptor.parse(skill);


    }
}
