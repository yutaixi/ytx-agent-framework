package com.ytx.ai.base.util;

public class StringUtils extends org.springframework.util.StringUtils{

    public static boolean isBlank(final CharSequence cs)
    {
        return !StringUtils.hasText(cs);
    }
}
