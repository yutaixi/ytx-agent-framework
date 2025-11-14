package com.ytx.ai.agent.repository.vo;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ScoredRecord<R> {

    private String label;

    private Float score;

    private R data;
}
