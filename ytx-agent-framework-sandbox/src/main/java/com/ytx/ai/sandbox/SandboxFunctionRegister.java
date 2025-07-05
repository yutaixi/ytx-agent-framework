package com.ytx.ai.sandbox;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SandboxFunctionRegister {

    private static final Map<String,SandboxFunction> functionMap=new ConcurrentHashMap<>();


    public static List<SandboxFunction> all(){
        return functionMap.values().stream().toList();
    }


    public static void register(SandboxFunction sandboxFunction){
        functionMap.put(sandboxFunction.getName(),sandboxFunction);
    }

}
