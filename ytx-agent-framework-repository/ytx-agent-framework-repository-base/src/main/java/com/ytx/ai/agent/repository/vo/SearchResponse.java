package com.ytx.ai.agent.repository.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class SearchResponse<R> {

    private List<ScoredRecord<R>> records;
}
