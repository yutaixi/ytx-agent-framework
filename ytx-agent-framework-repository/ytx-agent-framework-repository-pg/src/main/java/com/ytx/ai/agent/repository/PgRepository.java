package com.ytx.ai.agent.repository;

import com.ytx.ai.agent.repository.builder.PgSqlBuilder;
import com.ytx.ai.agent.repository.config.PgRepositoryConfig;
import com.ytx.ai.agent.repository.converter.PgObjectConverter;
import com.ytx.ai.agent.repository.converter.PgVectorConverter;
import com.ytx.ai.agent.repository.exception.PgDeleteException;
import com.ytx.ai.agent.repository.exception.PgInsertException;
import com.ytx.ai.agent.repository.vo.Filter;
import com.ytx.ai.agent.repository.vo.Node;
import com.ytx.ai.agent.repository.vo.ScoredRecord;
import com.ytx.ai.agent.repository.vo.SearchRequest;
import com.ytx.ai.agent.repository.vo.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.function.Consumer;
import java.util.Arrays;

/**
 * PostgreSQL Repository 实现
 * 重构后的版本，职责分离，使用依赖注入
 */
@Component
public class PgRepository<T extends Node> implements Repository<T> {

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final PgSqlBuilder sqlBuilder;
    private final PgObjectConverter objectConverter;
    private final PgVectorConverter vectorConverter;
    private final PgRepositoryConfig config;

    @Autowired
    public PgRepository(NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate,
                       PgSqlBuilder sqlBuilder,
                       PgObjectConverter objectConverter,
                       PgVectorConverter vectorConverter,
                       PgRepositoryConfig config) {
        this.jdbcTemplate = pgNamedParameterJdbcTemplate;
        this.sqlBuilder = sqlBuilder;
        this.objectConverter = objectConverter;
        this.vectorConverter = vectorConverter;
        this.config = config;
    }

    @Override
    public boolean createData(T data, String index) {
        if (data == null) {
            return false;
        }
        return batchCreateData(Collections.singletonList(data), index, false);
    }

    @Override
    public boolean batchCreateData(List<T> data, String index) {
        return batchCreateData(data, index, false);
    }

    @Override
    public boolean batchCreateData(List<T> data, String index, Boolean nodup) {
        if (CollectionUtils.isEmpty(data) || index == null || index.isEmpty()) {
            return false;
        }

        try {
            // 转换每个对象为 Map，并收集列名
            List<Map<String, Object>> records = new ArrayList<>(data.size());
            Set<String> columns = new LinkedHashSet<>();
            for (T item : data) {
                Map<String, Object> map = objectConverter.beanToMap(item);
                if (map.isEmpty()) {
                    continue;
                }
                records.add(map);
                columns.addAll(map.keySet());
            }

            if (records.isEmpty()) {
                return false;
            }

            // 插入策略：
            // - 数据库主键为 id；
            // - 业务唯一索引为 bid；
            // - 当 nodup=true 时，基于 bid 去重（ON CONFLICT (bid) DO NOTHING）；
            // - 当 nodup=false 时，执行普通 INSERT（不带冲突子句）。
            boolean hasBid = columns.contains(config.getBusinessIdField());

            // 构建 SQL
            String sql = sqlBuilder.buildInsertSql(index, columns, Boolean.TRUE.equals(nodup), hasBid);

            // 构建参数数组
            MapSqlParameterSource[] batchParams = new MapSqlParameterSource[records.size()];
            for (int i = 0; i < records.size(); i++) {
                MapSqlParameterSource params = new MapSqlParameterSource();
                Map<String, Object> record = records.get(i);
                for (String col : columns) {
                    params.addValue(col, record.get(col));
                }
                batchParams[i] = params;
            }

            int[] results = jdbcTemplate.batchUpdate(sql, batchParams);
            int total = Arrays.stream(results).sum();
            return total > 0;
        } catch (Exception ex) {
            throw new PgInsertException("pg batch insert error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteData(List<String> indexes, List<Filter> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            throw new PgDeleteException("delete node: missing filter fields.");
        }
        if (CollectionUtils.isEmpty(indexes)) {
            throw new PgDeleteException("delete node: missing index labels.");
        }

        int total = 0;
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            String where = sqlBuilder.buildWhereClause(filters, params, false); // allowEmpty = false

            for (String idx : indexes) {
                String sql = "DELETE FROM " + sqlBuilder.escapeIdentifier(idx) + " " + where;
                int affected = jdbcTemplate.update(sql, params.getValues());
                total += affected;
            }
            return total > 0;
        } catch (Exception ex) {
            throw new PgDeleteException("pg delete error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public SearchResponse<?> searchScroll(SearchRequest searchRequest, Consumer<Object> consumer) {
        return null;
    }

    @Override
    public SearchResponse<?> search(SearchRequest searchRequest) {
        if (searchRequest == null || searchRequest.getLabels() == null || searchRequest.getLabels().length == 0) {
            return SearchResponse.<T>builder().records(Collections.emptyList()).build();
        }

        List<ScoredRecord<T>> allRecords = new ArrayList<>();

        // 检测是否有 SIMILAR 过滤器
        Filter similarFilter = null;
        if (searchRequest.getFilters() != null) {
            for (Filter f : searchRequest.getFilters()) {
                if (f != null && Filter.ComparisonOpt.SIMILAR.equals(f.getComparisonOpt())) {
                    similarFilter = f;
                    break; // 只处理第一个 SIMILAR 过滤器
                }
            }
        }

        // 构建 SELECT 字段列表
        String selectFields = sqlBuilder.buildSelectFields(searchRequest.getReturnFields());

        // 如果有 SIMILAR 过滤器，在 SELECT 中添加距离计算
        String distanceAlias = "distance";
        if (similarFilter != null && similarFilter.getField() != null && similarFilter.getValue() != null) {
            String vectorField = sqlBuilder.escapeIdentifier(similarFilter.getField());
            // 将向量值转换为 PostgreSQL vector 格式的字符串
            String vectorStr = vectorConverter.convertVectorValueToString(similarFilter.getValue());
            // 在 SELECT 中添加距离计算（距离越小表示越相似，后续会转换为相似度分数）
            selectFields = selectFields + ", (" + vectorField + " <-> ARRAY[" + vectorStr + "]::vector) AS " + distanceAlias;
        }

        // 构建 ORDER BY 子句
        String orderBy = sqlBuilder.buildOrderByClause(searchRequest.getSort());
        // 如果有 SIMILAR 过滤器且用户没有指定排序，则按距离升序排序（距离越小越相似，后续会转换为相似度分数）
        if (similarFilter != null && (searchRequest.getSort() == null || searchRequest.getSort().trim().isEmpty())) {
            orderBy = "ORDER BY " + distanceAlias + " ASC";
        }

        // 构建 LIMIT 子句（先对每个表应用较大的 limit，最后再统一限制）
        int limit = searchRequest.getLimit() > 0 ? searchRequest.getLimit() : 5;
        // 为了确保能获取足够的结果，每个表查询时使用更大的 limit
        int perTableLimit = limit * searchRequest.getLabels().length;

        // 对每个表分别查询，然后合并结果
        for (String table : searchRequest.getLabels()) {
            try {
                // 为每个表创建新的参数源，避免参数名冲突
                MapSqlParameterSource params = new MapSqlParameterSource();

                // 构建 WHERE 子句（search 场景允许空条件，表示查询所有数据）
                String where = sqlBuilder.buildWhereClause(searchRequest.getFilters(), params, true); // allowEmpty = true

                String sql = "SELECT " + selectFields + " FROM " + sqlBuilder.escapeIdentifier(table) + " " + where + " " + orderBy + " LIMIT :limit";
                params.addValue("limit", perTableLimit);

                List<Map<String, Object>> rows = jdbcTemplate.query(sql, params.getValues(),
                        (RowMapper<Map<String, Object>>) (rs, rowNum) -> {
                            Map<String, Object> row = new LinkedHashMap<>();
                            int columnCount = rs.getMetaData().getColumnCount();
                            for (int i = 1; i <= columnCount; i++) {
                                String columnName = rs.getMetaData().getColumnLabel(i);
                                Object value = rs.getObject(i);
                                row.put(columnName, value);
                            }
                            return row;
                        });

                // 将每行数据转换为对象并构建 ScoredRecord
                for (Map<String, Object> row : rows) {
                    try {
                        // 提取距离值并转换为相似度分数（如果有）
                        Float similarity = null;
                        if (similarFilter != null && row.containsKey(distanceAlias)) {
                            Object distObj = row.get(distanceAlias);
                            if (distObj != null) {
                                Float distance;
                                if (distObj instanceof Number) {
                                    distance = ((Number) distObj).floatValue();
                                } else {
                                    distance = Float.parseFloat(String.valueOf(distObj));
                                }
                                // 将距离转换为相似度分数
                                similarity = vectorConverter.distanceToSimilarity(distance);
                            }
                            // 从 row 中移除 distance，避免影响数据转换
                            row.remove(distanceAlias);
                        }

                        T data = objectConverter.mapToBean(row, searchRequest.getClazz());
                        if (data != null) {
                            ScoredRecord<T> record = new ScoredRecord<>();
                            record.setLabel(table);
                            record.setScore(similarity); // 存储相似度分数（越大越相似，范围0-1）
                            record.setData(data);
                            allRecords.add(record);
                        }
                    } catch (Exception ex) {
                        // 忽略无法转换的行，继续处理下一行
                        continue;
                    }
                }
            } catch (Exception ex) {
                // 如果某个表查询失败，继续处理其他表
                continue;
            }
        }

        // 如果有排序字段，对合并后的结果再次排序
        if (searchRequest.getSort() != null && !searchRequest.getSort().trim().isEmpty()) {
            String[] sortParts = searchRequest.getSort().trim().split("\\s+");
            if (sortParts.length > 0) {
                String sortField = sortParts[0];
                boolean desc = sortParts.length > 1 && "DESC".equalsIgnoreCase(sortParts[1]);
                allRecords.sort((r1, r2) -> {
                    try {
                        Object v1 = objectConverter.getFieldValue(r1.getData(), sortField);
                        Object v2 = objectConverter.getFieldValue(r2.getData(), sortField);
                        if (v1 == null && v2 == null) return 0;
                        if (v1 == null) return desc ? 1 : -1;
                        if (v2 == null) return desc ? -1 : 1;
                        @SuppressWarnings("unchecked")
                        Comparable<Object> c1 = (Comparable<Object>) v1;
                        int cmp = c1.compareTo(v2);
                        return desc ? -cmp : cmp;
                    } catch (Exception ex) {
                        return 0;
                    }
                });
            }
        } else if (similarFilter != null) {
            // 如果没有指定排序但使用了 SIMILAR，按相似度分数降序排序（分数越大越相似）
            allRecords.sort((r1, r2) -> {
                Float s1 = r1.getScore();
                Float s2 = r2.getScore();
                if (s1 == null && s2 == null) return 0;
                if (s1 == null) return 1;  // null 值排在后面
                if (s2 == null) return -1; // null 值排在后面
                return Float.compare(s2, s1); // 降序：分数大的在前
            });
        }

        // 如果结果超过 limit，截取前 limit 条
        if (allRecords.size() > limit) {
            allRecords = allRecords.subList(0, limit);
        }

        return SearchResponse.<T>builder().records(allRecords).build();
    }

    @Override
    public boolean createIndex(String index, Class<?> clazz) {
        return false;
    }

    @Override
    public boolean createIndex(String index, String describeJson) {
        return false;
    }

    @Override
    public void deleteIndex(String index) {
        // 未实现
    }

    @Override
    public boolean addAlias(String index, String... alias) {
        return false;
    }

    @Override
    public Set<String> getAlias(String index) {
        return Set.of();
    }

    @Override
    public boolean delAlias(String index, String... alias) {
        return false;
    }

    @Override
    public boolean replaceAlias(String index, String oldAlias, String newAlias) {
        return false;
    }
}
