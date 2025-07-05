package com.ytx.ai.sandbox;

import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class Args {

    private Map<String,Object> params;


    public static Args of(){
        return new Args();
    }

    public static Args of(Map<String,Object> argsMap){
        Args args=new Args();
        args.setParams(argsMap);
        return args;
    }

    public Args bind(String name,Object value){
        if(params==null){
            params=new HashMap<>();
        }
        params.put(name,value);
        return this;
    }
}
