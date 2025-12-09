package com.ytx.ai.workflow.node;

import cn.hutool.core.util.ObjectUtil;
import com.ytx.ai.workflow.NodeMeta;
import com.ytx.ai.workflow.Value;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class NodeOutput {

    // 节点的输出值，对外使用
    private Map<String, Value> data;

    private NodeMeta nodeMeta;

    // 对话类流程，输出文字结果
    private String answer;

    public static NodeOutput of() {
        NodeOutput nodeOutput = new NodeOutput();
        nodeOutput.setData(new HashMap<>());
        return nodeOutput;
    }

    public void addData(String name, Value value) {
        if (ObjectUtil.isNull(data)) {
            data = new HashMap<>();
        }
        data.put(name, value);
    }

    public Value getDataValue(String name) {
        if (ObjectUtil.isNull(data)) {
            return null;
        }
        return data.get(name);
    }
}
