package com.ytx.ai.test;

import cn.hutool.json.JSONUtil;
import com.ytx.ai.workflow.WorkflowOutput;
import org.junit.Test;

public class TestHutool{


    @Test
    public void test_json_ignore(){
        WorkflowOutput workflowOutput = new WorkflowOutput();
        workflowOutput.setAnswer("ssfsd");
        workflowOutput.setCostSummary("1122222222");

        System.out.println(JSONUtil.toJsonStr(workflowOutput));
    }
}
