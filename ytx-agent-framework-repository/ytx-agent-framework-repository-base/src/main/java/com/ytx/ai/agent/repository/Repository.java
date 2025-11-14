package com.ytx.ai.agent.repository;

import com.ytx.ai.agent.repository.vo.Filter;
import com.ytx.ai.agent.repository.vo.Node;
import com.ytx.ai.agent.repository.vo.SearchRequest;
import com.ytx.ai.agent.repository.vo.SearchResponse;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public interface Repository<T extends Node> {


    public boolean createData(T data,String index);

    public boolean batchCreateData(List<T> data, String index);

    public boolean batchCreateData(List<T> data,String index,Boolean nodup);

    public boolean deleteData(List<String> indexes,List<Filter> filters);

    public SearchResponse<?> searchScroll(SearchRequest searchRequest, Consumer<Object> consumer);

    public SearchResponse<?> search(SearchRequest searchRequest);

    public boolean createIndex(String index,Class<?> clazz);

    public boolean createIndex(String index,String describeJson);


    public void deleteIndex(String index);

    public boolean addAlias(String index,String... alias);

    public Set<String> getAlias(String index);

    public boolean delAlias(String index,String... alias);

    public boolean replaceAlias(String index,String oldAlias,String newAlias);

}
