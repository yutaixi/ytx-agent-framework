package com.ytx.ai.parser.vo;

import com.ytx.ai.base.util.GzipUtils;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ImageVO {

    private int index;

    private byte[] bytes;

    private String ext;

    public byte[] getBytes() {
        return GzipUtils.uncompress(bytes);
    }

    public void setBytes(byte[] bytes) {
        this.bytes =GzipUtils.compress(bytes);
    }
}