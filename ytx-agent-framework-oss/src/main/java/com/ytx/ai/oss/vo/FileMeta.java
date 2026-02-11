package com.ytx.ai.oss.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class FileMeta{
    private String name;
    private String path;
    private String suffix;
    private String mimeType;
    private long size;
    private String storeType;
}