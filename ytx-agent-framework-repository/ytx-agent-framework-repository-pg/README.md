# ytx-agent-framework-repository-pg

PostgreSQL Repository 实现模块，为 ytx-agent-framework 提供基于 PostgreSQL 的数据存储和检索能力，支持向量相似度搜索、复杂条件查询等功能。

## 功能特性

### 核心功能

- ✅ **数据操作**
  - 单条/批量数据插入
  - 支持去重插入（基于业务ID）
  - 条件删除
  - 多表联合查询

- ✅ **向量搜索**
  - 基于 pgvector 的向量相似度搜索
  - 自动距离转相似度分数计算
  - 支持向量字段的自动转换

- ✅ **查询条件**
  - 支持多种比较操作符：
    - `EQUAL` - 等于
    - `LIKE` - 模糊匹配
    - `REGEXP` - 正则表达式
    - `GT/GTE` - 大于/大于等于
    - `LT/LTE` - 小于/小于等于
    - `BETWEEN` - 范围查询
    - `SIMILAR` - 向量相似度搜索

- ✅ **架构设计**
  - 职责分离，高内聚低耦合
  - 策略模式处理查询条件
  - 依赖注入，易于测试和扩展
  - 完善的异常处理机制

## 依赖要求

### 数据库要求

- PostgreSQL 12+
- pgvector 扩展（用于向量搜索）

### Maven 依赖

```xml
<dependency>
    <groupId>com.ytx.ai</groupId>
    <artifactId>ytx-agent-framework-repository-pg</artifactId>
    <version>${revision}</version>
</dependency>
```

### 核心依赖

- `postgresql` (42.7.8) - PostgreSQL JDBC 驱动
- `pgvector` (0.1.6) - pgvector 支持
- `spring-boot-starter-data-jpa` - Spring Data JPA
- `ytx-agent-framework-repository-base` - Repository 基础接口

## 配置说明

### 启用模块

在 `application.yml` 或 `application.properties` 中配置：

```yaml
ai:
  knowledge:
    repository:
      type: postgresql  # 启用 PostgreSQL Repository
```

### 数据源配置

```yaml
spring:
  datasource:
    postgresql:
      url: jdbc:postgresql://localhost:5432/your_database
      username: your_username
      password: your_password
      driver-class-name: org.postgresql.Driver
```

### Repository 配置

```yaml
pg:
  repository:
    primary-key-field: id      # 主键字段名，默认: id
    business-id-field: bid     # 业务唯一标识字段名，默认: bid
```

## 使用示例

### 1. 数据插入

```java
@Autowired
private PgRepository<Node> repository;

// 单条插入
Node node = new Node();
node.setBid("business-id-001");
node.setContent("示例内容");
repository.createData(node, "table_name");

// 批量插入
List<Node> nodes = Arrays.asList(node1, node2, node3);
repository.batchCreateData(nodes, "table_name");

// 批量插入（去重）
repository.batchCreateData(nodes, "table_name", true);
```

### 2. 向量相似度搜索

```java
// 构建搜索请求
SearchRequest request = SearchRequest.builder()
    .labels(new String[]{"table1", "table2"})  // 要搜索的表
    .filters(Arrays.asList(
        Filter.builder()
            .field("embedding")  // 向量字段名
            .comparisonOpt(Filter.ComparisonOpt.SIMILAR)
            .value(queryVector)  // 查询向量（float[] 或 List<Float>）
            .build()
    ))
    .limit(10)
    .clazz(Node.class)
    .build();

// 执行搜索
SearchResponse<Node> response = repository.search(request);

// 处理结果
for (ScoredRecord<Node> record : response.getRecords()) {
    System.out.println("相似度: " + record.getScore());
    System.out.println("数据: " + record.getData());
}
```

### 3. 条件查询

```java
// 构建查询条件
List<Filter> filters = Arrays.asList(
    // 等于条件
    Filter.builder()
        .field("status")
        .comparisonOpt(Filter.ComparisonOpt.EQUAL)
        .value("active")
        .build(),

    // 模糊匹配
    Filter.builder()
        .field("content")
        .comparisonOpt(Filter.ComparisonOpt.LIKE)
        .value("%关键词%")
        .build(),

    // 范围查询
    Filter.builder()
        .field("score")
        .comparisonOpt(Filter.ComparisonOpt.BETWEEN)
        .value(Arrays.asList(0.5, 1.0))
        .build()
);

SearchRequest request = SearchRequest.builder()
    .labels(new String[]{"table_name"})
    .filters(filters)
    .sort("created_time DESC")
    .limit(20)
    .clazz(Node.class)
    .build();

SearchResponse<Node> response = repository.search(request);
```

### 4. 数据删除

```java
List<Filter> filters = Arrays.asList(
    Filter.builder()
        .field("id")
        .comparisonOpt(Filter.ComparisonOpt.EQUAL)
        .value("record-id-001")
        .build()
);

repository.deleteData(
    Arrays.asList("table1", "table2"),  // 要删除的表
    filters
);
```

## 架构设计

### 核心组件

```
PgRepository (主类)
├── PgSqlBuilder (SQL 构建器)
│   ├── ConditionHandlerRegistry (条件处理器注册表)
│   └── PgIdentifierEscaper (标识符转义器)
├── PgObjectConverter (对象转换器)
│   └── FieldTypeResolver (字段类型解析器)
├── PgVectorConverter (向量转换器)
│   └── SimilarityCalculator (相似度计算器)
└── PgRepositoryConfig (配置类)
```

### 条件处理器（策略模式）

- `EqualConditionHandler` - 等于条件
- `LikeConditionHandler` - 模糊匹配
- `RegexpConditionHandler` - 正则表达式
- `GtConditionHandler` / `GteConditionHandler` - 大于/大于等于
- `LtConditionHandler` / `LteConditionHandler` - 小于/小于等于
- `BetweenConditionHandler` - 范围查询
- `SimilarConditionHandler` - 向量相似度（占位处理器）

### 数据流程

1. **插入流程**
   ```
   Node对象 → PgObjectConverter → Map → PgSqlBuilder → SQL → JDBC执行
   ```

2. **搜索流程**
   ```
   SearchRequest → PgSqlBuilder → SQL → JDBC查询 → Map → PgObjectConverter → Node对象
   ```

3. **向量搜索流程**
   ```
   查询向量 → PgVectorConverter → PostgreSQL vector → pgvector距离计算 → 相似度转换 → ScoredRecord
   ```

## API 说明

### Repository 接口方法

| 方法 | 说明 |
|------|------|
| `createData(T data, String index)` | 单条数据插入 |
| `batchCreateData(List<T> data, String index)` | 批量数据插入 |
| `batchCreateData(List<T> data, String index, Boolean nodup)` | 批量插入（支持去重） |
| `deleteData(List<String> indexes, List<Filter> filters)` | 条件删除 |
| `search(SearchRequest searchRequest)` | 搜索查询 |
| `searchScroll(SearchRequest searchRequest, Consumer<Object> consumer)` | 滚动搜索（未实现） |

### Filter 比较操作符

| 操作符 | 说明 | 值类型 |
|--------|------|--------|
| `EQUAL` | 等于 | 任意 |
| `LIKE` | 模糊匹配 | String |
| `REGEXP` | 正则表达式 | String |
| `GT` | 大于 | 数值 |
| `GTE` | 大于等于 | 数值 |
| `LT` | 小于 | 数值 |
| `LTE` | 小于等于 | 数值 |
| `BETWEEN` | 范围查询 | List（两个元素） |
| `SIMILAR` | 向量相似度 | float[] / List<Float> |

## 注意事项

### 1. 向量字段要求

- 向量字段必须是 PostgreSQL 的 `vector` 类型
- 查询向量支持 `float[]`、`Float[]`、`List<Float>` 等格式
- 向量维度需要一致

### 2. 表结构要求

- 建议包含主键字段（默认：`id`）
- 建议包含业务唯一标识字段（默认：`bid`），用于去重插入
- 向量字段类型为 `vector`

示例表结构：

```sql
CREATE TABLE example_table (
    id BIGSERIAL PRIMARY KEY,
    bid VARCHAR(255) UNIQUE,
    content TEXT,
    embedding vector(1536),  -- 向量字段，维度根据实际情况调整
    created_time TIMESTAMP DEFAULT NOW()
);

-- 创建向量索引（可选，用于加速相似度搜索）
CREATE INDEX ON example_table USING ivfflat (embedding vector_cosine_ops);
```

### 3. 去重插入

- 当 `nodup=true` 时，使用 `ON CONFLICT (bid) DO NOTHING`
- 需要表中有 `bid` 字段且设置了唯一约束
- 如果表中没有 `bid` 字段，将执行普通插入

### 4. 多表查询

- `search` 方法支持多表查询（通过 `labels` 参数指定多个表）
- 每个表独立查询，结果合并后统一排序和限制
- 如果某个表查询失败，会继续处理其他表

### 5. 异常处理

模块定义了以下异常类型：

- `PgRepositoryException` - 基础异常
- `PgInsertException` - 插入异常
- `PgDeleteException` - 删除异常
- `PgSearchException` - 搜索异常
- `PgConversionException` - 转换异常

## 扩展开发

### 添加新的查询条件

1. 实现 `ConditionHandler` 接口：

```java
@Component
public class CustomConditionHandler implements ConditionHandler {
    @Override
    public String buildCondition(Filter filter, MapSqlParameterSource params, String paramBase) {
        // 实现条件构建逻辑
        String paramName = paramBase + "_" + filter.getField();
        params.addValue(paramName, filter.getValue());
        return filter.getField() + " custom_operator :" + paramName;
    }
}
```

2. 注册到 `ConditionHandlerRegistry`（通过 Spring 自动扫描）

### 自定义相似度计算

实现 `SimilarityCalculator` 接口：

```java
@Component
public class CustomSimilarityCalculator implements SimilarityCalculator {
    @Override
    public float calculate(float distance) {
        // 自定义相似度计算逻辑
        return 1.0f / (1.0f + distance);
    }
}
```

## 版本历史

- **1.0-SNAPSHOT** - 初始版本
  - 实现基础 CRUD 操作
  - 支持向量相似度搜索
  - 支持多种查询条件
  - 职责分离的架构设计

## 相关文档

- [PgRepository 优化分析](../../../doc/design/PgRepository优化分析.md)
- [PgRepository 重构示例](../../../doc/design/PgRepository重构示例.md)
- [主项目 README](../../../README.md)

## 许可证

与主项目保持一致。

