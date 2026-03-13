## ytx-agent-framework

**一套面向生产环境的 AI Agent 框架，内置记忆、工具、工作流与多模态文档解析能力，帮助你在最短时间内搭建符合业务需求的智能体应用。**

> 已基于本框架开发的示例应用 👉 [ytx-agent](https://github.com/yutaixi/ytx-agent)

---

## 特性一览

- **模块化 Agent 能力模型**：抽象统一的 `Agent` 能力接口，支持多种角色与技能组合，内置 LLM 服务封装，统一管理温度、系统提示等参数。
- **工作流编排引擎**：支持将复杂业务拆分为多个 `FlowNode` / `FlowEdge`，既支持配置化 `workflow`，也支持 `native` 代码工作流。
- **文档解析与多模态支持**：Java + Rust 组合实现高性能文档解析，支持 PDF / Word / Excel / TXT，多路输出结构化文本和页面截图 / 合成长图，便于对接多模态模型。
- **数据库与持久化能力**：通用 Repository 抽象 + PostgreSQL 实现模块，提供条件构建器和统一异常封装。
- **多租户 / 多 Agent 隔离**：基于请求拦截器透传 `tenantCode` / `agentCode` / `openAuthToken` 等上下文，适配 B 端多租户场景。
- **沙盒执行环境**：内置 Sandbox 模块与 Rust `ytx_sandbox`，用于安全地执行扩展逻辑，通过 JNI 与 Java 集成。
- **生产级技术栈**：JDK 21、Spring Boot 3.5.x、Spring Cloud 2025.x、Spring AI、MyBatis-Plus 3.5.x，可选 GraalVM 原生镜像。

---

## 架构设计

[![架构图](https://s21.ax1x.com/2024/09/02/pAVNqu6.jpg)](https://imgse.com/i/pAVNqu6)

---

## 模块概览

项目为多模块 Maven 工程，核心模块如下（见根 `pom.xml` 的 `<modules>`）：

- **`ytx-agent-framework-base`**：基础模型与通用枚举（如 `Flow`、`SystemClock`、`MemorySaveOption` 等），为其他模块提供统一定义。
- **`ytx-agent-framework-core`**：Agent 核心能力与配置（如 `DefaultAgentHolder`、`AgentProperty` 等），负责 Agent 生命周期与注册管理。
- **`ytx-agent-framework-dao`**：DAO 层与数据访问服务（如 `AgentService`、`DBBasedAgentHolder` 等）。
- **`ytx-agent-framework-llm`**：LLM 集成模块，封装大模型交互配置与 VO（如 `LlmConfig`、`LlmChatCompletion` 等）。
- **`ytx-agent-framework-workflow`**：工作流引擎模块，定义 `Workflow`、`WorkflowWrapper`、`BasicNode` 以及执行相关工具类。
- **`ytx-agent-framework-log`**：日志与审计相关能力（按实际实现补充）。
- **`ytx-agent-framework-web`**：Web 接入与拦截器，如 `RequestContextInterceptor` 提供多租户 / Agent 上下文透传。
- **`ytx-agent-framework-repository` / `ytx-agent-framework-repository-base` / `ytx-agent-framework-repository-pg`**：通用 Repository 抽象与 PostgreSQL 实现，封装条件构建器、异常类型等。
- **`ytx-agent-framework-parser`**：文档解析模块，如 `PdfDocumentParser` 支持提取 PDF 文本与页面图片。
- **`ytx-agent-framework-sandbox`**：Java 侧 Sandbox，配合 Rust `ytx_sandbox` 实现安全扩展执行环境。
- **`ytx-agent-framework-oss`**：对象存储相关支持（按实际实现补充，例如知识库文件持久化）。
- **`rust-lib/*`**：Rust 扩展库，包括文档解析、LLM 封装、独立 Sandbox CLI 等，通过 JNI 与 Java 框架集成。

> 建议：实际开源前，可以在各模块子目录下补充更细的 `README.md`，对具体能力做深入说明。

---

## 快速开始

### 环境要求

- **JDK**：21+
- **Maven**：3.9+
- **数据库**：PostgreSQL（如使用 `ytx-agent-framework-repository-pg`）
- **可选（增强能力）**：
  - Rust 工具链（构建 `rust-lib`）
  - GraalVM（如需原生镜像）

### 1. 克隆仓库

```bash
git clone https://github.com/你的账号/ytx-agent-framework.git
cd ytx-agent-framework
```

### 2. 编译与测试

```bash
mvn clean install -DskipTests=false
```

### 3. 启动基于本框架的应用

本仓库主要提供通用 Agent 能力与工作流框架本身，实际应用可以参考：

- 示例应用 👉 [ytx-agent](https://github.com/yutaixi/ytx-agent)

在示例应用中，你可以看到：

- 如何在配置中声明 Agent。
- 如何编排工作流节点。
- 如何调用文档解析、知识库等能力。

### 4. 在你的项目中引入依赖

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>com.ytx.ai</groupId>
            <artifactId>ytx-agent-framework</artifactId>
            <version>1.0-SNAPSHOT</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependencies>
    <!-- 按需引入核心能力 -->
    <dependency>
        <groupId>com.ytx.ai</groupId>
        <artifactId>ytx-agent-framework-core</artifactId>
    </dependency>
    <dependency>
        <groupId>com.ytx.ai</groupId>
        <artifactId>ytx-agent-framework-workflow</artifactId>
    </dependency>
    <dependency>
        <groupId>com.ytx.ai</groupId>
        <artifactId>ytx-agent-framework-llm</artifactId>
    </dependency>
    <!-- 其他模块按需引入 -->
</dependencies>
```

---

## 核心概念速览

### Agent（智能体）

- `Agent` 是框架的核心抽象，负责：
  - 接收外部输入（请求参数、上下文）。
  - 调用 LLM、工具链、文档解析等能力。
  - 产出结构化的任务结果 / 回复内容。
- `BaseAgent` 等实现中注入了 `LlmService`，统一管理模型参数（如 temperature）等。

### Workflow（工作流）

- `Workflow` 用于描述一条完整的业务流程，由多个 `FlowNode` 和 `FlowEdge` 组成：
  - `Workflow.of(Flow flow)`：可从持久化的 `Flow` 定义构建运行时工作流。
  - 支持 `type = "workflow"` 的配置化流程，也预留 `native` 流程扩展入口。
- 典型使用场景：
  - 将「检索-解析-调用工具-生成回复」拆分为多个可维护的节点。
  - 支持水平扩展 / 动态调整节点配置。

### 文档解析（Parser）

以 `PdfDocumentParser` 为例：

- **输入**：`DocumentParseOption`（包含文件字节流、解析页数上限、默认图片格式、缩放参数等）。
- **输出**：`DocumentVO`，包含：
  - 提取后的全文 `text`。
  - 每页的 `ImageVO`（截图字节、索引、扩展名）。
  - 可选的合成长图 `mergedImage`。
- **内部实现要点**：
  - 使用 `PDFBox` 提取文本与渲染页面。
  - 自动计算 DPI 与缩放比例，控制图片宽度与大小。
  - 支持按页限制、自动合并图片等选项。

---

## 多租户与请求上下文

`ytx-agent-framework-web` 模块中的 `RequestContextInterceptor`：

- 在 `preHandle` 中从请求头读取：
  - `tenantCode`（租户编码）。
  - `agentCode`（Agent 编码）。
  - `openAuthToken`（开放鉴权 Token）。
- 写入到 `RequestContext` 中，供后续服务链路使用。
- 在 `afterCompletion` 中清理上下文，避免线程复用带来的数据污染。

这使得框架天然适配 SaaS / 多租户场景，可以在同一服务实例中为不同租户和 Agent 提供隔离的上下文。

---

## Rust 扩展说明（简要）

`rust-lib` 目录下包含多个 Rust 子 crate，例如：

- `file-parser`：高性能文档解析（PDF / Word / Excel / TXT）。
- `llm`：LLM 调用能力封装（可按需对接不同模型供应商）。
- `ytx_sandbox`：独立的 Sandbox 运行时，用于执行不可信或扩展脚本。

Java 侧通过 JNI 与这些 Rust 模块集成，在保持良好开发体验的同时，获得更高的性能与更低的资源消耗。

> 如果你暂时不需要极致性能，可以只使用纯 Java 模块；Rust 扩展是可选增强能力。

---

## 适用场景

- **企业内部知识库问答 / 智能助理**：结合文档解析、工作流和多租户上下文，为不同业务条线提供定制 Agent。
- **SaaS 型智能体平台**：基于多租户隔离与统一 Repository 抽象，为外部客户托管专属 Agent。
- **复杂业务流程自动化**：将人工流程拆分到工作流节点中，交由 Agent 驱动执行和决策。
- **多模态检索与阅读理解**：支持将长文档拆页、合成长图，作为输入提供给多模态大模型。

---

## 路线图（Roadmap）

> 可根据实际进度更新，这里仅给出示例方向：

- [ ] 完善各模块单元测试与集成测试。
- [ ] 提供更多内置 Agent 模板（检索式问答 / 工单助手 / 数据分析等）。
- [ ] 丰富工作流节点类型（条件分支、并行节点、子流程等）。
- [ ] 完成更多云厂商 / 模型供应商的适配。
- [ ] 提供前端控制台（Agent / Workflow 可视化管理）。

---

## 贡献指南

欢迎 Issue / PR！

- **提 Bug**：请带上复现步骤、日志和相关模块信息。
- **提需求 / 讨论设计**：可以先开一个 Discussion 或 Issue 描述你的场景。
- **提交代码**：
  - 保持模块职责清晰，高内聚、低耦合，避免写死业务逻辑。
  - 新增 / 修改代码请补充注释与必要测试用例。
  - 命名需体现业务含义，便于理解。

---

## License

> 根据你实际选择的开源协议来补充，例如：

本项目计划采用 `MIT` / `Apache-2.0` 协议开源（TODO：待最终确认）。

---

## English Overview (Short)

**ytx-agent-framework** is a modular AI Agent framework built on Spring Boot and Spring AI.  
It provides:

- Agent abstraction with memory, tools and workflow orchestration.
- Document parsing (PDF / Word / Excel / TXT) and multimodal support.
- Repository abstraction with PostgreSQL implementation.
- Multi-tenant web integration and sandboxed extensions (Java + Rust).

See the [ytx-agent](https://github.com/yutaixi/ytx-agent) repo for a concrete application built on top of this framework.
