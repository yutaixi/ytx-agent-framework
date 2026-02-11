package com.ytx.ai.agent.llm.vo.billing;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ytx.ai.agent.llm.vo.CompletionTokensDetails;
import lombok.Data;

/**
 * @author plexpt
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Usage {
    @JsonProperty("prompt_tokens")
    private long promptTokens;
    @JsonProperty("completion_tokens")
    private long completionTokens;
    @JsonProperty("total_tokens")
    private long totalTokens;

    @JsonIgnoreProperties(ignoreUnknown = true)
    private CompletionTokensDetails completion_tokens_details;


}


