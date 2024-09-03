package com.ytx.ai.agent.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class UserIntention {
    String intent;
    String tag;
    String reply;
    String isClear;
    String reason;

}
