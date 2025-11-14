package com.ytx.ai.agent.repository.enums;

import lombok.Getter;

@Getter
public enum ErrorMsgEnum {
    INDEX_NOT_EXIST("92001", "index not exist");

    private final String code;
    private final String desc;

    ErrorMsgEnum(String code, String desc) {
        this.code = code;
        this.desc = desc;
    }
}
