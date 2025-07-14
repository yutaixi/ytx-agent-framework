package com.ytx.ai.web.vo;

import lombok.Getter;

@Getter
public enum Status {

    SUCCESS(200),
    SYSTEM_EXCEPTION(500),
    //业务异常
    SERVICE_EXCEPTION(10001);

    private final Integer code;
    private Status(Integer code){
        this.code = code;
    }
}
