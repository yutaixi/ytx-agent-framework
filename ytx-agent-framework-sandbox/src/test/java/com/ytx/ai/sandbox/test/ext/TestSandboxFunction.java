package com.ytx.ai.sandbox.test.ext;


import com.ytx.ai.sandbox.SandboxFunction;
import org.graalvm.polyglot.HostAccess;

public class TestSandboxFunction implements SandboxFunction {

    public static final String NAME = "testFunction";

    @HostAccess.Export
    public String run(String url) {
        return url + " request body";
    }

    @Override
    @HostAccess.Export
    public String getName() {
        return NAME;
    }
}
