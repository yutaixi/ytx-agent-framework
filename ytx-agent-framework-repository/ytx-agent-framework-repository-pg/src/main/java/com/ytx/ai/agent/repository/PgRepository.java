package com.ytx.ai.agent.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ytx.ai.agent.repository.vo.Filter;
import com.ytx.ai.agent.repository.vo.Node;
import com.ytx.ai.agent.repository.vo.ScoredRecord;
import com.ytx.ai.agent.repository.vo.SearchRequest;
import com.ytx.ai.agent.repository.vo.SearchResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Array;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.postgresql.util.PGobject;

public class PgRepository<T extends Node> implements Repository<T> {

    @Autowired
    private NamedParameterJdbcTemplate pgNamedParameterJdbcTemplate;

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

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
                Map<String, Object> map = beanToMap(item);
                if (map.isEmpty())
                    continue;
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
            boolean hasBid = columns.contains("bid");

            // 构建 SQL
            String sql = buildInsertSql(index, columns, Boolean.TRUE.equals(nodup), hasBid);

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

            int[] results = pgNamedParameterJdbcTemplate.batchUpdate(sql, batchParams);
            int total = Arrays.stream(results).sum();
            return total > 0;
        } catch (Exception ex) {
            throw new RuntimeException("pg batch insert error: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean deleteData(List<String> indexes, List<Filter> filters) {
        if (CollectionUtils.isEmpty(filters)) {
            throw new RuntimeException("delete node: missing filter fields.");
        }
        if (CollectionUtils.isEmpty(indexes)) {
            throw new RuntimeException("delete node: missing index labels.");
        }

        int total = 0;
        try {
            MapSqlParameterSource params = new MapSqlParameterSource();
            String where = buildWhereClause(filters, params);
            for (String idx : indexes) {
                String sql = "DELETE FROM " + escapeIdentifier(idx) + " " + where;
                int affected = pgNamedParameterJdbcTemplate.update(sql, params.getValues());
                total += affected;
            }
            return total > 0;
        } catch (Exception ex) {
            throw new RuntimeException("pg delete error: " + ex.getMessage(), ex);
        }
    }

    /*
     * helper: 构建 WHERE 子句（带 WHERE 前缀），并把参数写入 params.
     * 语义对齐 EsRepository：
     * - AND 条件作为 must；
     * - OR 条件作为 should，若存在 AND，则 should 为可选（即忽略 should）；
     * - 只有 OR 且无 AND 时，使用 OR 组合；
     * - 若最终没有任何有效条件，则抛异常，避免误删全表。
     */
    private String buildWhereClause(List<Filter> filters, MapSqlParameterSource params) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        List<String> andParts = new ArrayList<>();
        List<String> orParts = new ArrayList<>();
        int idx = 0;
        for (Filter f : filters) {
            String cond = buildCondition(f, params, idx++);
            if (cond == null || cond.isEmpty()) {
                continue;
            }
            if (f.getLogicalOpt() != null && Filter.LogicalOpt.AND.equalsIgnoreCase(f.getLogicalOpt())) {
                andParts.add(cond);
            } else {
                orParts.add(cond);
            }
        }
        if (andParts.isEmpty() && orParts.isEmpty()) {
            throw new RuntimeException("delete node: invalid filters lead to empty conditions.");
        }
        if (!andParts.isEmpty()) {
            // 对齐 ES 语义：存在 AND（must）时忽略 OR（should，默认可选）
            String where = andParts.stream().map(p -> "(" + p + ")").collect(Collectors.joining(" AND "));
            return "WHERE " + where;
        } else {
            String where = orParts.stream().map(p -> "(" + p + ")").collect(Collectors.joining(" OR "));
            return "WHERE " + where;
        }
    }

    private String buildCondition(Filter filter, MapSqlParameterSource params, int idx) {
        if (filter == null || filter.getField() == null || filter.getComparisonOpt() == null) {
            return "";
        }
        String field = escapeIdentifier(filter.getField());
        String cmp = filter.getComparisonOpt();
        String pnameBase = "p" + idx;
        switch (cmp) {
            case Filter.ComparisonOpt.EQUAL:
                Object val = filter.getValue();
                if (val instanceof Collection) {
                    String paramName = pnameBase + "_in";
                    params.addValue(paramName, val);
                    return field + " IN (:" + paramName + ")";
                } else if (val != null && val.getClass().isArray()) {
                    String paramName = pnameBase + "_in";
                    params.addValue(paramName, val);
                    return field + " IN (:" + paramName + ")";
                } else {
                    String paramName = pnameBase;
                    params.addValue(paramName, val);
                    return field + " = :" + paramName;
                }
            case Filter.ComparisonOpt.LIKE:
                String paramLike = pnameBase;
                params.addValue(paramLike, "%" + String.valueOf(filter.getValue()) + "%");
                // 使用 ILIKE 做不区分大小写匹配
                return field + " ILIKE :" + paramLike;
            case Filter.ComparisonOpt.REGEXP:
                String paramReg = pnameBase;
                params.addValue(paramReg, filter.getValue());
                // PostgreSQL 正则匹配运算符 ~ (区分大小写) 或 ~*（不区分大小写），这里用 ~
                return field + " ~ :" + paramReg;
            case Filter.ComparisonOpt.GT:
                params.addValue(pnameBase, filter.getValue());
                return field + " > :" + pnameBase;
            case Filter.ComparisonOpt.GTE:
                params.addValue(pnameBase, filter.getValue());
                return field + " >= :" + pnameBase;
            case Filter.ComparisonOpt.LT:
                params.addValue(pnameBase, filter.getValue());
                return field + " < :" + pnameBase;
            case Filter.ComparisonOpt.LTE:
                params.addValue(pnameBase, filter.getValue());
                return field + " <= :" + pnameBase;
            case Filter.ComparisonOpt.BETWEEN:
                Object v = filter.getValue();
                Object lo = null;
                Object hi = null;
                if (v instanceof Collection) {
                    Iterator<?> it = ((Collection<?>) v).iterator();
                    if (it.hasNext())
                        lo = it.next();
                    if (it.hasNext())
                        hi = it.next();
                } else if (v != null && v.getClass().isArray()) {
                    Object[] arr = (Object[]) v;
                    if (arr.length > 0)
                        lo = arr[0];
                    if (arr.length > 1)
                        hi = arr[1];
                }
                if (lo == null || hi == null) {
                    throw new RuntimeException("between search need two params.");
                }
                String pLo = pnameBase + "_lo";
                String pHi = pnameBase + "_hi";
                params.addValue(pLo, lo);
                params.addValue(pHi, hi);
                return field + " BETWEEN :" + pLo + " AND :" + pHi;
            case Filter.ComparisonOpt.SIMILAR:
                // 向量相似度搜索：返回总是为真的条件，实际排序在 search 方法中处理
                // 向量值会在 search 方法中使用，这里只返回一个占位条件
                return "1=1";
            default:
                return "";
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
        String selectFields = buildSelectFields(searchRequest.getReturnFields());

        // 如果有 SIMILAR 过滤器，在 SELECT 中添加距离计算
        String distanceAlias = "distance";
        if (similarFilter != null && similarFilter.getField() != null && similarFilter.getValue() != null) {
            String vectorField = escapeIdentifier(similarFilter.getField());
            // 将向量值转换为 PostgreSQL vector 格式的字符串
            String vectorStr = convertVectorValueToString(similarFilter.getValue());
            // 在 SELECT 中添加距离计算（距离越小表示越相似，后续会转换为相似度分数）
            selectFields = selectFields + ", (" + vectorField + " <-> ARRAY[" + vectorStr + "]::vector) AS " + distanceAlias;
        }

        // 构建 ORDER BY 子句
        String orderBy = buildOrderByClause(searchRequest.getSort());
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
                String where = buildWhereClauseForSearch(searchRequest.getFilters(), params);

                String sql = "SELECT " + selectFields + " FROM " + escapeIdentifier(table) + " " + where + " " + orderBy + " LIMIT :limit";
                params.addValue("limit", perTableLimit);

                List<Map<String, Object>> rows = pgNamedParameterJdbcTemplate.query(sql, params.getValues(),
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
                                // 将距离转换为相似度分数：1 - distance
                                // 距离为0时相似度为1，距离为1时相似度为0
                                // 如果距离可能大于1，需要确保结果在0-1范围内
                                similarity = Math.max(0.0f, Math.min(1.0f, 1.0f - distance));
                            }
                            // 从 row 中移除 distance，避免影响数据转换
                            row.remove(distanceAlias);
                        }

                        T data = mapToBean(row, searchRequest.getClazz());
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
                        Object v1 = getFieldValue(r1.getData(), sortField);
                        Object v2 = getFieldValue(r2.getData(), sortField);
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

    // --- helper methods ---

    private Map<String, Object> beanToMap(T bean) {
        if (bean == null)
            return Collections.emptyMap();
        Map<String, Object> raw = MAPPER.convertValue(bean, new TypeReference<Map<String, Object>>() {
        });
        // 清理 null 值并将复杂对象序列化为 JSON 字符串
        Map<String, Object> cleaned = new LinkedHashMap<>();
        for (Map.Entry<String, Object> e : raw.entrySet()) {
            String key = e.getKey();
            Object val = e.getValue();
            if (val == null)
                continue;

            // 处理向量字段（vector 类型）：根据值的类型自动识别，不依赖字段名
            if (isEmbeddingValue(val)) {
                cleaned.put(key, convertToVectorPGobject(val));
            } else if (isSimpleValue(val)) {
                cleaned.put(key, val);
            } else {
                try {
                    cleaned.put(key, MAPPER.writeValueAsString(val));
                } catch (JsonProcessingException ex) {
                    // fallback to toString
                    cleaned.put(key, String.valueOf(val));
                }
            }
        }
        return cleaned;
    }

    /**
     * 检查值是否为 embedding（向量）类型
     */
    private boolean isEmbeddingValue(Object val) {
        return val instanceof List<?> || val instanceof float[] || val instanceof Float[] || val instanceof double[] || val instanceof Double[];
    }

    /**
     * 将 embedding 值转换为 PostgreSQL vector 类型的 PGobject
     */
    private PGobject convertToVectorPGobject(Object val) {
        try {
            float[] floatArray = convertToPrimitiveFloatArray(val);
            // pgvector 格式: [1.0,2.0,3.0]
            StringBuilder sb = new StringBuilder();
            sb.append("[");
            for (int i = 0; i < floatArray.length; i++) {
                if (i > 0) {
                    sb.append(",");
                }
                sb.append(floatArray[i]);
            }
            sb.append("]");

            PGobject pgObject = new PGobject();
            pgObject.setType("vector");
            pgObject.setValue(sb.toString());
            return pgObject;
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to convert embedding to vector: " + ex.getMessage(), ex);
        }
    }

    private boolean isSimpleValue(Object val) {
        return val instanceof CharSequence
                || val instanceof Number
                || val instanceof Boolean
                || val instanceof Date
                || val instanceof UUID;
    }

    private String buildInsertSql(String table, Set<String> columns, boolean nodup, boolean hasBid) {
        String cols = String.join(", ", columns);
        String namedCols = columns.stream().map(c -> ":" + c).collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder();
        sb.append("INSERT INTO ").append(escapeIdentifier(table))
                .append(" (").append(cols).append(") ")
                .append("VALUES (").append(namedCols).append(") ");

        if (nodup) {
            if (!hasBid) {
                throw new RuntimeException("nodup insert requires unique business id field 'bid'.");
            }
            List<String> updateCols = columns.stream()
                    .filter(c -> !"bid".equalsIgnoreCase(c) && !"id".equalsIgnoreCase(c))
                    .collect(Collectors.toList());
            if (updateCols.isEmpty()) {
                sb.append("ON CONFLICT (").append("bid").append(") DO NOTHING");
            } else {
                String updateClause = updateCols.stream()
                        .map(c -> c + " = EXCLUDED." + c)
                        .collect(Collectors.joining(", "));
                sb.append("ON CONFLICT (").append("bid").append(") DO UPDATE SET ").append(updateClause);
            }
        }
        return sb.toString();
    }

    /*
     * helper: 构建 WHERE 子句（用于 search，允许空条件）
     * 语义对齐 EsRepository：
     * - AND 条件作为 must；
     * - OR 条件作为 should，若存在 AND，则 should 为可选（即忽略 should）；
     * - 只有 OR 且无 AND 时，使用 OR 组合；
     * - 若最终没有任何有效条件，返回空字符串（表示查询所有数据）。
     */
    private String buildWhereClauseForSearch(List<Filter> filters, MapSqlParameterSource params) {
        if (filters == null || filters.isEmpty()) {
            return "";
        }
        List<String> andParts = new ArrayList<>();
        List<String> orParts = new ArrayList<>();
        int idx = 0;
        for (Filter f : filters) {
            String cond = buildCondition(f, params, idx++);
            if (cond == null || cond.isEmpty()) {
                continue;
            }
            if (f.getLogicalOpt() != null && Filter.LogicalOpt.AND.equalsIgnoreCase(f.getLogicalOpt())) {
                andParts.add(cond);
            } else {
                orParts.add(cond);
            }
        }
        if (andParts.isEmpty() && orParts.isEmpty()) {
            // search 场景允许空条件，返回空字符串
            return "";
        }
        if (!andParts.isEmpty()) {
            // 对齐 ES 语义：存在 AND（must）时忽略 OR（should，默认可选）
            String where = andParts.stream().map(p -> "(" + p + ")").collect(Collectors.joining(" AND "));
            return "WHERE " + where;
        } else {
            String where = orParts.stream().map(p -> "(" + p + ")").collect(Collectors.joining(" OR "));
            return "WHERE " + where;
        }
    }

    /*
     * helper: 构建 SELECT 字段列表
     * 如果 returnFields 为空，返回 *（所有字段）
     */
    private String buildSelectFields(String[] returnFields) {
        if (returnFields == null || returnFields.length == 0) {
            return "*";
        }
        return Arrays.stream(returnFields)
                .map(this::escapeIdentifier)
                .collect(Collectors.joining(", "));
    }

    /*
     * helper: 构建 ORDER BY 子句
     * 如果 sort 为空，返回空字符串（不排序）
     */
    private String buildOrderByClause(String sort) {
        if (sort == null || sort.trim().isEmpty()) {
            return "";
        }
        // 简单处理：假设 sort 格式为 "field" 或 "field ASC/DESC"
        String[] parts = sort.trim().split("\\s+");
        if (parts.length == 0) {
            return "";
        }
        String field = escapeIdentifier(parts[0]);
        String direction = parts.length > 1 && "DESC".equalsIgnoreCase(parts[1]) ? "DESC" : "ASC";
        return "ORDER BY " + field + " " + direction;
    }

    /*
     * helper: 将 Map 转换为 Bean 对象
     * 忽略 Map 中存在但 clazz 中不存在的字段
     */
    @SuppressWarnings("unchecked")
    private T mapToBean(Map<String, Object> map, Class<?> clazz) {
        if (map == null || clazz == null) {
            return null;
        }
        try {
            Map<String, Object> sanitized = new LinkedHashMap<>(map.size());
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                String fieldName = entry.getKey();
                Object value = entry.getValue();
                Object converted = convertValueForField(clazz, fieldName, value);
                sanitized.put(fieldName, converted);
            }
            return (T) MAPPER.convertValue(sanitized, clazz);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to convert map to bean: " + ex.getMessage(), ex);
        }
    }

    private Object convertValueForField(Class<?> clazz, String fieldName, Object value) {
        if (value == null || fieldName == null || clazz == null) {
            return value;
        }
        Class<?> fieldType = resolveFieldType(clazz, fieldName);
        if (fieldType == null) {
            return value;
        }
        if (fieldType.isArray() && fieldType.getComponentType() == float.class) {
            return convertToPrimitiveFloatArray(value);
        }
        if (Float[].class.equals(fieldType)) {
            float[] primitive = convertToPrimitiveFloatArray(value);
            Float[] boxed = new Float[primitive.length];
            for (int i = 0; i < primitive.length; i++) {
                boxed[i] = primitive[i];
            }
            return boxed;
        }
        // 处理 List<Float> 或 ArrayList<Float> 类型
        if (List.class.isAssignableFrom(fieldType) || Collection.class.isAssignableFrom(fieldType)) {
            Type genericType = resolveFieldGenericType(clazz, fieldName);
            if (genericType != null && genericType instanceof ParameterizedType) {
                ParameterizedType paramType = (ParameterizedType) genericType;
                Type[] actualTypes = paramType.getActualTypeArguments();
                if (actualTypes.length > 0 && actualTypes[0] == Float.class) {
                    // 字段类型是 List<Float> 或 ArrayList<Float>
                    // 如果值是 Map 类型（可能是 PGobject 被序列化后的结果），尝试提取值
                    if (value instanceof Map) {
                        // 尝试从 Map 中提取值，可能是 {"type":"vector","value":"[1.0,2.0,3.0]"} 格式
                        Map<?, ?> map = (Map<?, ?>) value;
                        if (map.containsKey("value")) {
                            value = map.get("value");
                        } else if (map.size() == 1 && map.values().iterator().hasNext()) {
                            // 如果只有一个值，尝试使用它
                            value = map.values().iterator().next();
                        } else {
                            // 如果 Map 包含多个键值对，尝试将其转换为数组
                            // 这种情况通常不应该发生，但为了健壮性，我们尝试处理
                            try {
                                value = MAPPER.writeValueAsString(value);
                            } catch (JsonProcessingException e) {
                                // 如果序列化失败，使用原始值
                            }
                        }
                    }
                    float[] primitive = convertToPrimitiveFloatArray(value);
                    List<Float> floatList = new ArrayList<>(primitive.length);
                    for (float f : primitive) {
                        floatList.add(f);
                    }
                    return floatList;
                }
            }
        }
        return value;
    }

    private Class<?> resolveFieldType(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName).getType();
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private Type resolveFieldGenericType(Class<?> clazz, String fieldName) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(fieldName).getGenericType();
            } catch (NoSuchFieldException ignored) {
            }
            current = current.getSuperclass();
        }
        return null;
    }

    private float[] convertToPrimitiveFloatArray(Object value) {
        if (value == null) {
            return new float[0];
        }
        if (value instanceof float[]) {
            return (float[]) value;
        }
        if (value instanceof Float[] floats) {
            float[] result = new float[floats.length];
            for (int i = 0; i < floats.length; i++) {
                result[i] = floats[i] == null ? 0f : floats[i];
            }
            return result;
        }
        if (value instanceof double[] doubles) {
            float[] result = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                result[i] = (float) doubles[i];
            }
            return result;
        }
        if (value instanceof Double[] doubles) {
            float[] result = new float[doubles.length];
            for (int i = 0; i < doubles.length; i++) {
                result[i] = doubles[i] == null ? 0f : doubles[i].floatValue();
            }
            return result;
        }
        if (value instanceof Collection<?> collection) {
            float[] result = new float[collection.size()];
            int idx = 0;
            for (Object item : collection) {
                result[idx++] = parseNumberToFloat(item);
            }
            return result;
        }
        if (value instanceof Object[] array) {
            float[] result = new float[array.length];
            for (int i = 0; i < array.length; i++) {
                result[i] = parseNumberToFloat(array[i]);
            }
            return result;
        }
        if (value instanceof Array sqlArray) {
            try {
                Object array = sqlArray.getArray();
                return convertToPrimitiveFloatArray(array);
            } catch (SQLException e) {
                throw new RuntimeException("Failed to read SQL array: " + e.getMessage(), e);
            }
        }
        if (value instanceof PGobject pgObject) {
            String type = pgObject.getType();
            if ("vector".equalsIgnoreCase(type) || "json".equalsIgnoreCase(type) || "jsonb".equalsIgnoreCase(type)) {
                return convertToPrimitiveFloatArray(pgObject.getValue());
            }
        }
        if (value instanceof String str) {
            return parseFloatVectorString(str);
        }
        return new float[] { parseNumberToFloat(value) };
    }

    private float parseNumberToFloat(Object value) {
        if (value == null) {
            return 0f;
        }
        if (value instanceof Number number) {
            return number.floatValue();
        }
        return Float.parseFloat(String.valueOf(value));
    }

    private float[] parseFloatVectorString(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new float[0];
        }
        String cleaned = text.trim();
        if ((cleaned.startsWith("[") && cleaned.endsWith("]")) || (cleaned.startsWith("{") && cleaned.endsWith("}"))) {
            cleaned = cleaned.substring(1, cleaned.length() - 1);
        }
        if (cleaned.isEmpty()) {
            return new float[0];
        }
        String[] parts = cleaned.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = parseNumberToFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * 将向量值转换为 PostgreSQL vector 格式的字符串
     * 例如：[0.02, 0.03, 0.04, 0.05] -> "0.02,0.03,0.04,0.05"
     */
    private String convertVectorValueToString(Object value) {
        if (value == null) {
            throw new RuntimeException("SIMILAR search: vector value cannot be null.");
        }
        float[] vector = convertToPrimitiveFloatArray(value);
        if (vector.length == 0) {
            throw new RuntimeException("SIMILAR search: vector value cannot be empty.");
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(vector[i]);
        }
        return sb.toString();
    }

    /*
     * helper: 从对象中获取字段值（用于排序）
     */
    private Object getFieldValue(Object obj, String fieldName) {
        if (obj == null || fieldName == null) {
            return null;
        }
        try {
            Map<String, Object> map = MAPPER.convertValue(obj, new TypeReference<Map<String, Object>>() {});
            return map.get(fieldName);
        } catch (Exception ex) {
            return null;
        }
    }

    private String escapeIdentifier(String id) {
        // 简单处理，若需要可以增强以防 SQL 注入
        if (id.contains("\"")) {
            id = id.replace("\"", "\"\"");
        }
        return "\"" + id + "\"";
    }
}
