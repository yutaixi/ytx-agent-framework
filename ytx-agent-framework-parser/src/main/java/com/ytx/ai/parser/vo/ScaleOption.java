package com.ytx.ai.parser.vo;

import lombok.Getter;
import lombok.Setter;

import static com.ytx.ai.parser.constant.ParseConstants.DEFAULT_IMAGE_MAX_MB;
import static com.ytx.ai.parser.constant.ParseConstants.IMAGE_MAX_DIMENSION;


@Getter
@Setter
public class ScaleOption {

    private boolean enable=true;
    /**
     * 缩放比例
     */
    private Double scale;
    /**
     * 是否保持原图比例
     */
    private boolean keepAspectRatio = true;
    /**
     * 是否使用高质量缩放
     */
    private boolean highQuality = true;
    private Integer maxDimension=IMAGE_MAX_DIMENSION;
    private Double maxMB=DEFAULT_IMAGE_MAX_MB;

}