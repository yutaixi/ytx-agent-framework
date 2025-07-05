package com.ytx.ai.agent.vo;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PageVO<T> {

    private long current;
    private long size=10;
    private long total;
    private List<T> data;

    public static <T> PageVO<T> of(Page<T> page){

        return PageVO.<T>builder().current(page.getCurrent()).size(page.getSize()).total(page.getTotal()).data(page.getRecords()).build();
    }

}
