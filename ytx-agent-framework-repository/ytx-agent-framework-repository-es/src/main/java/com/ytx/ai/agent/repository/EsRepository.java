package com.ytx.ai.agent.repository;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.document.Document;
import org.springframework.data.elasticsearch.core.index.AliasAction;
import org.springframework.data.elasticsearch.core.index.AliasActionParameters;
import org.springframework.data.elasticsearch.core.index.AliasActions;
import org.springframework.data.elasticsearch.core.index.AliasData;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.ByQueryResponse;
import org.springframework.data.elasticsearch.core.query.DeleteQuery;
import org.springframework.data.elasticsearch.core.query.FetchSourceFilterBuilder;
import org.springframework.data.elasticsearch.core.query.UpdateQuery;

import com.google.common.base.Preconditions;
import com.ytx.ai.agent.repository.vo.Filter;
import com.ytx.ai.agent.repository.vo.Node;
import com.ytx.ai.agent.repository.vo.ScoredRecord;
import com.ytx.ai.agent.repository.vo.SearchRequest;
import com.ytx.ai.agent.repository.vo.SearchResponse;

import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.json.JSONUtil;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.json.JsonData;

public class EsRepository<T extends Node> implements Repository<T> {

    @Autowired
    private ElasticsearchOperations elasticsearchOperations;

    @Override
    public boolean createData(T data, String index) {
        return batchCreateData(List.of(data), index);
    }

    @Override
    public boolean batchCreateData(List<T> data, String index) {
        if (ObjectUtil.isEmpty(data)) {
            return false;
        }

        List<UpdateQuery> queries = new ArrayList<>();
        data.forEach(item -> {
            Document nodeDocument = Document.create().fromJson(JSONUtil.toJsonStr(item));
            UpdateQuery createQuery = UpdateQuery.builder(item.getBid())
                    .withDocAsUpsert(true)
                    .withDocument(nodeDocument)
                    .build();
            queries.add(createQuery);
        });

        elasticsearchOperations.bulkUpdate(queries, IndexCoordinates.of(index));
        return true;
    }

    @Override
    public boolean batchCreateData(List<T> data, String index, Boolean nodup) {
        if (ObjectUtil.isEmpty(data)) {
            return false;
        }

        if (!nodup) {
            return batchCreateData(data, index);
        }

        List<UpdateQuery> queries = new ArrayList<>();
        data.forEach(item -> {
            Document nodeDocument = Document.create().fromJson(JSONUtil.toJsonStr(item));
            UpdateQuery createQuery = UpdateQuery.builder(item.getBid())
                    .withDocAsUpsert(true)
                    .withDocument(nodeDocument)
                    .build();
            queries.add(createQuery);
        });

        elasticsearchOperations.bulkUpdate(queries, IndexCoordinates.of(index));
        return true;
    }

    @Override
    public boolean deleteData(List<String> indexes, List<Filter> filters) {
        // 删除必须有筛选条件
        if (ObjectUtil.isEmpty(filters)) {
            throw new RuntimeException("delete node: missing filter fields.");
        }

        // 待删除节点的索引
        IndexCoordinates nodeIndex = IndexCoordinates.of(ArrayUtil.toArray(indexes, String.class));
        BoolQuery.Builder nodeFilterQueryBuilder = QueryBuilders.bool();
        filters.forEach(filter -> {
            if (Filter.LogicalOpt.AND.equalsIgnoreCase(filter.getLogicalOpt())) {
                nodeFilterQueryBuilder.must(buildComparisonQuery(filter, Integer.MAX_VALUE));
            } else {
                nodeFilterQueryBuilder.should(buildComparisonQuery(filter, Integer.MAX_VALUE));
            }
        });

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> {
                    return q.bool(nodeFilterQueryBuilder.build());
                })
                .build();

        DeleteQuery deleteQuery = DeleteQuery.builder(query).build();
        ByQueryResponse response = elasticsearchOperations.delete(deleteQuery, null, nodeIndex);
        return ObjectUtil.isEmpty(response.getFailures());
    }

    @Override
    public SearchResponse<?> searchScroll(SearchRequest searchRequest, Consumer<Object> consumer) {
        return null;
    }

    @Override
    public SearchResponse<?> search(SearchRequest request) {
        IndexCoordinates dataIndexes = IndexCoordinates.of(request.getLabels());

        BoolQuery.Builder nodeFilterQueryBuilder = QueryBuilders.bool();
        if (ObjectUtil.isNotEmpty(request.getFilters())) {
            request.getFilters().forEach(filter -> {
                if (Filter.LogicalOpt.AND.equalsIgnoreCase(filter.getLogicalOpt())) {
                    nodeFilterQueryBuilder.must(buildComparisonQuery(filter, request.getLimit()));
                } else {
                    nodeFilterQueryBuilder.should(buildComparisonQuery(filter, request.getLimit()));
                }
            });
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(q -> q.bool(nodeFilterQueryBuilder.build()))
                .withMaxResults(request.getLimit())
                .withSort(s -> s.score(ss -> ss.order(SortOrder.Desc)))
                .build();

        if (ObjectUtil.isNotEmpty(request.getReturnFields())) {
            query.addSourceFilter(new FetchSourceFilterBuilder()
                    .withIncludes(request.getReturnFields())
                    .build());
        }

        SearchHits<T> searchHits = (SearchHits<T>) elasticsearchOperations.search(query, request.getClazz(),
                dataIndexes);
        List<ScoredRecord<T>> records = new ArrayList<>();
        searchHits.stream().forEach(searchHit -> {
            ScoredRecord<T> record = new ScoredRecord<>();
            record.setScore(searchHit.getScore());
            record.setLabel(searchHit.getIndex());
            T data = searchHit.getContent();
            record.setData(data);
            records.add(record);
        });

        return SearchResponse.<T>builder().records(records).build();
    }

    private List<FieldValue> parseFiledValues(Object value) {
        List<FieldValue> filedValues = new ArrayList<>();

        if (ObjectUtil.isEmpty(value)) {
            return filedValues;
        }

        if (value instanceof Collection<?> listValue) {
            listValue.forEach(item -> {
                FieldValue fieldValue = FieldValue.of(item);
                filedValues.add(fieldValue);
            });
        } else if (value instanceof Object[] arrayValue) {
            Arrays.stream(arrayValue).forEach(item -> {
                FieldValue fieldValue = FieldValue.of(item);
                filedValues.add(fieldValue);
            });
        } else {
            FieldValue fieldValue = FieldValue.of(value);
            filedValues.add(fieldValue);
        }

        return filedValues;
    }

    private List<Query> buildComparisonQuery(Filter filter, int limit) {
        Query query = null;

        switch (filter.getComparisonOpt()) {
            case Filter.ComparisonOpt.EQUAL:
                List<FieldValue> filedValues = parseFiledValues(filter.getValue());
                query = QueryBuilders.terms(t -> {
                    return t.field(filter.getField()).terms(tv -> tv.value(filedValues));
                });
                break;

            case Filter.ComparisonOpt.LIKE:
                query = QueryBuilders.match(t -> {
                    return t.field(filter.getField()).query(FieldValue.of(filter.getValue()));
                });
                break;

            case Filter.ComparisonOpt.REGEXP:
                query = QueryBuilders.regexp(t -> {
                    return t.field(filter.getField()).value(filter.getValue().toString());
                });
                break;

            case Filter.ComparisonOpt.GT:
                query = QueryBuilders.range(t -> {
                    return t.untyped(tt -> {
                        return tt.field(filter.getField()).gt(JsonData.of(filter.getValue()));
                    });
                });
                break;

            case Filter.ComparisonOpt.GTE:
                query = QueryBuilders.range(t -> {
                    return t.untyped(tt -> {
                        return tt.field(filter.getField()).gte(JsonData.of(filter.getValue()));
                    });
                });
                break;

            case Filter.ComparisonOpt.LT:
                query = QueryBuilders.range(t -> {
                    return t.untyped(tt -> {
                        return tt.field(filter.getField()).lt(JsonData.of(filter.getValue()));
                    });
                });
                break;

            case Filter.ComparisonOpt.LTE:
                query = QueryBuilders.range(t -> {
                    return t.untyped(tt -> {
                        return tt.field(filter.getField()).lte(JsonData.of(filter.getValue()));
                    });
                });
                break;

            case Filter.ComparisonOpt.BETWEEN:
                List<FieldValue> betweenValues = parseFiledValues(filter.getValue());
                if (ObjectUtil.isEmpty(betweenValues) || betweenValues.size() != 2) {
                    throw new RuntimeException("between search need two params.");
                }
                query = QueryBuilders.range(t -> {
                    return t.untyped(tt -> {
                        return tt.field(filter.getField())
                                .gte(JsonData.of(betweenValues.getFirst()._get()))
                                .lte(JsonData.of(betweenValues.getLast()._get()));
                    });
                });
                break;

            case Filter.ComparisonOpt.SIMILAR:
                query = QueryBuilders.knn(t -> {
                    List<Float> embeddings = (List<Float>) filter.getValue();
                    return t.field(filter.getField())
                            .queryVector(embeddings)
                            .k(limit)
                            .numCandidates(limit + 5);
                });
                break;

            default:
                break;
        }

        return ListUtil.toList(query);
    }

    @Override
    public boolean createIndex(String index, Class<?> clazz) {

        if (!elasticsearchOperations.indexOps(clazz).exists()) {
            elasticsearchOperations.indexOps(clazz).create();
        }
        elasticsearchOperations.indexOps(clazz).putMapping();
        return false;
    }

    @Override
    public boolean createIndex(String index, String describeJson) {
        Preconditions.checkNotNull(index, "Index name cannot be null");
        Preconditions.checkNotNull(describeJson, "Describe JSON cannot be null");

        try {
            // 解析 JSON 字符串
            Map<String, Object> jsonMap = JSONUtil.parseObj(describeJson);

            // 获取 IndexOperations
            IndexOperations indexOperations = elasticsearchOperations.indexOps(IndexCoordinates.of(index));

            // 检查索引是否已存在
            if (indexOperations.exists()) {
                return true; // 索引已存在，返回成功
            }

            // 提取 settings
            if (jsonMap.containsKey("settings")) {
                Object settingsObj = jsonMap.get("settings");
                String settingsJson = JSONUtil.toJsonStr(settingsObj);
                // 使用 Document 解析 settings JSON
                Document settingsDocument = Document.parse(settingsJson);
                // 创建索引（带 settings）
                indexOperations.create(settingsDocument);
            } else {
                // 创建索引（不带 settings）
                indexOperations.create();
            }

            // 提取并设置 mappings
            if (jsonMap.containsKey("mappings")) {
                Object mappingsObj = jsonMap.get("mappings");
                String mappingsJson = JSONUtil.toJsonStr(mappingsObj);
                Document mappingDocument = Document.parse(mappingsJson);
                indexOperations.putMapping(mappingDocument);
            }

            return true;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create index from JSON: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteIndex(String index) {
        IndexOperations indexOperations = elasticsearchOperations.indexOps(IndexCoordinates.of(index));
        if (!indexOperations.exists()) {
            return;
        }
        indexOperations.delete();
    }

    @Override
    public boolean addAlias(String index, String... alias) {
        Preconditions.checkNotNull(index);
        Preconditions.checkNotNull(alias);

        final IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(index));
        if (!indexOps.exists()) {
            throw new RuntimeException("Index \"" + index + "\" not exist.");
        }

        AliasActions aliasActions = new AliasActions(new AliasAction.Add(
                AliasActionParameters.builder()
                        .withIndices(index)
                        .withAliases(alias)
                        .build()));
        return indexOps.alias(aliasActions);
    }

    @Override
    public Set<String> getAlias(String index) {
        Preconditions.checkNotNull(index);

        final IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(index));
        final Map<String, Set<AliasData>> aliases = indexOps.getAliasesForIndex(index);
        final Set<AliasData> dataSet = aliases.get(index);

        if (ObjectUtil.isEmpty(dataSet)) {
            return new HashSet<>();
        }

        Set<String> set = new HashSet<>(dataSet.size());
        dataSet.forEach(aliasData -> set.add(aliasData.getAlias()));
        return set;
    }

    @Override
    public boolean delAlias(String index, String... alias) {
        Preconditions.checkNotNull(index);
        Preconditions.checkNotNull(alias);

        final IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(index));
        AliasActions aliasActions = new AliasActions(new AliasAction.Remove(
                AliasActionParameters.builder()
                        .withIndices(index)
                        .withAliases(alias)
                        .build()));
        return indexOps.alias(aliasActions);
    }

    @Override
    public boolean replaceAlias(String index, String oldAlias, String newAlias) {
        Preconditions.checkNotNull(index);
        Preconditions.checkNotNull(oldAlias);
        Preconditions.checkNotNull(newAlias);

        final IndexOperations indexOps = elasticsearchOperations.indexOps(IndexCoordinates.of(index));
        final AliasAction.Add add = new AliasAction.Add(AliasActionParameters.builder()
                .withIndices(index)
                .withAliases(newAlias)
                .build());
        final AliasAction.Remove remove = new AliasAction.Remove(AliasActionParameters.builder()
                .withIndices(index)
                .withAliases(oldAlias)
                .build());

        AliasActions aliasActions = new AliasActions(add, remove);
        return indexOps.alias(aliasActions);
    }
}
