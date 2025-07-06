package com.ytx.ai.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
@Builder
@AllArgsConstructor
public class ValueSource {

    private String type;//ref,literal
    private String nId;
    private String vName;
    private String vGroup;
}