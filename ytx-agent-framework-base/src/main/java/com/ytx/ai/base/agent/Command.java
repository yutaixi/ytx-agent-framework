package com.ytx.ai.base.agent;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Command {

    private String name;
    private String objective;
    private String value;
}
