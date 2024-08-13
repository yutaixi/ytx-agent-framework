package com.ytx.ai.agent.vo;

import com.ytx.ai.agent.enums.MemorySaveOption;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MemoryTrace {

    String name;
    String value;
    MemorySaveOption saveOption;
    boolean userAssigned;
    private boolean hasSaved;

}
