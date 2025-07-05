package com.ytx.ai.sandbox;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class Output {

    private Map<String,Object> valueMap;

    public static Output of(){
        return new Output();
    }


    public static Output of(Map<String,Object> valueMap){
        Output output=new Output();
        output.setValueMap(valueMap);
        return output;
    }

    public Object get(String name){
        if(valueMap==null){
            return null;
        }
        return valueMap.get(name);
    }

    public Boolean getBool(String name){

        Object value=get(name);
        if(value==null){
            return null;
        }
        return Boolean.valueOf(value.toString());
    }

    public String getString(String name){

        Object value=get(name);
        if(value==null){
            return null;
        }
        return value.toString();
    }

    public Integer getInt(String name){

        Object value=get(name);
        if(value==null){
            return null;
        }
        return Integer.valueOf(value.toString());
    }

}
