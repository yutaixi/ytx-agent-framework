package com.ytx.ai.base.util;

public class ResponseUtils {


    public static final String getJsonData(String content)
    {
        if(content.startsWith("```json")){
            content=content.substring(7);
            content=content.substring(0,content.length()-3);
        }
        return content;
    }

}
