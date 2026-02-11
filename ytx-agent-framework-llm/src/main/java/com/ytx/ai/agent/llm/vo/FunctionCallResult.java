package com.ytx.ai.agent.llm.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FunctionCallResult {

    String name;

    String arguments;
}