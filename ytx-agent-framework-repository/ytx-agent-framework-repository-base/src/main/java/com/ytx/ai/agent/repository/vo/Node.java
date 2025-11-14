package com.ytx.ai.agent.repository.vo;

import com.ytx.ai.agent.repository.util.RepositoryUtils;

public interface Node {

    public String getBid();

    default public String getLabel(){
        return RepositoryUtils.getIndexName(this);
    }
}
