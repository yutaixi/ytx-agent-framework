package com.ytx.ai.agent.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Command {

    String name;
    String objective;
    String value;
}
