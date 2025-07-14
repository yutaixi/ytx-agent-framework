package com.ytx.ai.agent.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PageSearchVO <T>{

    private T data;
    private int size=10;
    private int current=1;
}
