package com.ytx.ai.agent.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
public class CrisisIdentification {

    private boolean isMatch;
    private String reply;
    private String reason;
}
