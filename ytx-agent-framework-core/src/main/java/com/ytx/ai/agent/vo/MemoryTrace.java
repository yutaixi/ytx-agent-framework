package com.ytx.ai.agent.vo;

import com.ytx.ai.agent.enums.MemorySaveOption;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class MemoryTrace {

    private String name;
    private String value;
    private MemorySaveOption saveOption;
    private boolean userAssigned;
    private boolean hasSaved;

}
