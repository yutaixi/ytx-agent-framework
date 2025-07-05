package com.ytx.ai.base.util;

import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.ytx.ai.base.service.Sequence;

public class IdGenerator {

    private static Sequence sequence;


    public static String next(){

        if(ObjectUtil.isNull(sequence)){
            synchronized(IdGenerator.class){
                if(ObjectUtil.isNull(sequence)){
                    sequence= SpringUtil.getBean(Sequence.class);
                }
            }
        }
        if(ObjectUtil.isNull(sequence)){
            throw new RuntimeException("sequence instance not found.");
        }

        return String.valueOf(sequence.nextId());

    }

}
