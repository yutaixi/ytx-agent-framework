package com.ytx.ai.agent.llm.vo;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class CompletionTokensDetails {

    Long reasoning_tokens;
}
