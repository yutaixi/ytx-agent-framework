package com.ytx.ai.agent.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import com.ytx.ai.agent.vo.AgentTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class TaskUtils {

    public static List<Map.Entry<Integer, List<AgentTask>>> reorderTasks(List<AgentTask> tasks){
        Map<Integer,List<AgentTask>> taskMap=new HashMap<>();
        tasks.forEach(task->{
            List<AgentTask> parallelTasks=taskMap.get(task.getExecute_order());

            if(ObjectUtil.isNotNull(parallelTasks)){
                parallelTasks.add(task);
            }else
            {
                parallelTasks=new ArrayList<>();
                parallelTasks.add(task);
            }
            taskMap.put(task.getExecute_order(),parallelTasks);
        });

        //将HashMap转换为List
        List<Map.Entry<Integer,List<AgentTask>>> list=new ArrayList<>(taskMap.entrySet());
        //根据key对list进行排序
        list.sort(Map.Entry.comparingByKey());
        //遍历排序后的List
        list.forEach(entry->log.debug(entry.getKey()+"->"+ JSONUtil.toJsonStr(entry.getValue())));
        return list;
    }

}
