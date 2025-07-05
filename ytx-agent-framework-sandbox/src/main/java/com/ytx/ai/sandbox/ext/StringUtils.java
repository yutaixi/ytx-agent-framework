package com.ytx.ai.sandbox.ext;


import cn.hutool.core.util.StrUtil;
import com.ytx.ai.sandbox.SandboxFunction;

import org.graalvm.polyglot.HostAccess;

public class StringUtils implements SandboxFunction {

    @Override
    @HostAccess.Export
    public String getName() {
        return "StringUtils";
    }

    @HostAccess.Export
    public boolean isBlank(CharSequence str){
        return StrUtil.isBlank(str);
    }

    @HostAccess.Export
    public boolean isNotBlank(CharSequence str){
        return StrUtil.isNotBlank(str);
    }
}
