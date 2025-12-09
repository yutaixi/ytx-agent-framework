package com.ytx.ai.workflow.node.agent;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.agent.service.AgentService;
import com.ytx.ai.base.agent.AgentResponse;
import com.ytx.ai.base.agent.ChatDTO;
import com.ytx.ai.base.agent.PlannedTasks;
import com.ytx.ai.workflow.FlowNode;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import com.ytx.ai.workflow.annotation.ContractOutputs;
import com.ytx.ai.workflow.annotation.DependsRef;
import com.ytx.ai.workflow.annotation.ExpandInputs;
import com.ytx.ai.workflow.enums.WorkflowPluginTypeIdEnum;
import com.ytx.ai.workflow.execute.AgentExecuteContext;
import com.ytx.ai.workflow.execute.AgentTaskExecutor;
import com.ytx.ai.workflow.execute.FlowContext;
import com.ytx.ai.workflow.node.BasicNode;
import com.ytx.ai.workflow.node.NodeOutput;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class AgentExecutorNode extends BasicNode {

    @Autowired
    private AgentService agentService;


    @Autowired
    private AgentTaskExecutor agentTaskExecutor;

    @Override
    public void init() {

    }

    @Override
    public String getType() {
        return WorkflowPluginTypeIdEnum.AGENT_EXECUTOR.getType();
    }

    @Override
    public NodeOutput doBiz(FlowNode flowNode, FlowContext flowContext) {
        System.out.println("AgentExecutorPlugin.doBiz");
        AgentExecutorNodeMeta meta=(AgentExecutorNodeMeta)flowNode.getMeta();
        PlannedTasks plannedTasks=meta.getPlannedTasks();
        if(ObjectUtil.isEmpty(plannedTasks) || ObjectUtil.isEmpty(plannedTasks.getTasks())){
            meta.setTaskResult(plannedTasks);
            return NodeOutput.of();
        }

        AgentExecuteContext context=new AgentExecuteContext();
        context.setSkillMap(flowContext.getSkillMap());
        AgentResponse response= agentTaskExecutor.run(meta.getUserInput(), meta.plannedTasks,context,false,30000L);
        meta.setTaskResult(plannedTasks);
        return NodeOutput.of();
    }

    @Override
    public Class<? extends NodeMeta> getMetaClass() {
        return AgentExecutorNodeMeta.class;
    }

    @Getter
    @Setter
    public static class AgentExecutorNodeMeta implements NodeMeta {

        @DependsRef
        private List<Value> inputs;
        private List<Value> outputs;

        @ExpandInputs
        private PlannedTasks plannedTasks;
        @ExpandInputs
        private ChatDTO userInput;

        @ContractOutputs
        private PlannedTasks taskResult;
    }
}
