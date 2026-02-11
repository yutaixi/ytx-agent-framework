package com.ytx.ai.workflow;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ValueSource {

    private String type;//ref,literal
    private String nId;
    private String vName;
    private String vGroup;
}