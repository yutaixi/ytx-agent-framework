package com.ytx.ai.agent.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserIntention {
    private String intent;
    private String tag;
    private String reply;
    private String isClear;
    private String reason;

}
