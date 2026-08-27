# Agent-A · 多模型智能代理（Agent）服务平台

> 基于 **Spring Boot 3 + Spring AI** 的可配置、可扩展、可观测的 Agent 运行时平台，
> 内置 **知识库 RAG（检索增强生成）** 能力，配套 Vue 3 管理后台，实现「对话模型 + 工具调用 + 知识库」的一站式智能体编排。

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-6db33f.svg)](https://spring.io/projects/spring-boot)
[![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1-6db33f.svg)](https://spring.io/projects/spring-ai)
[![Vue](https://img.shields.io/badge/Vue-3.5-4fc08d.svg)](https://vuejs.org/)
[![Vite](https://img.shields.io/badge/Vite-6-646cff.svg)](https://vitejs.dev/)

---

## 一、项目简介

Agent-A 是一个面向业务场景的 **LLM Agent 落地实践项目**，核心目标是把「大模型 + 工具调用 + 私有知识库」三者通过配置化、可视化的方式组织起来：

- **多模型路由**：统一走 OpenAI 兼容协议，一行配置即可接入 DeepSeek / Kimi / 硅基流动等任意模型；
- **Agent 运行时**：Agent 定义、System Prompt、工具绑定全部存库，启动时动态组装为可执行实例，支持热加载；
- **工具自动发现**：业务方只需给方法加 `@Tool` 注解，平台自动扫描、注册、绑定，无需手工维护元数据；
- **知识库 RAG**：文档解析 → 文本分块 → 向量化 → ES 检索 → 上下文自动注入，一条完整链路；
- **会话记忆与观测**：Redis 多轮记忆、会话隔离、调用日志、Prompt 版本历史。

项目内置两个可直接对话的示例 Agent，开箱即用：

| Agent | 编码 | 说明 |
|-------|------|------|
| 🧋 蜜雪冰城点单助手 | `order_helper` | 演示业务工具编排：定位 → 登录 → 菜单推荐 → 下单 → 订单查询 |
| 🛠️ 系统运维助手 | `ops_helper` | 演示运维工具：Redis 监控、JVM 监控、CPU 核数、日期计算 |

---

## 二、核心特性

- ✅ **配置驱动的多模型路由**：新增模型只需在 `application-llm.yml` 加一条配置，无需改任何 Java 代码；
- ✅ **Agent 动态组装 + 热加载**：修改 Prompt / 工具绑定后，调用 `/agent/{code}/reload` 即时生效，无需重启；
- ✅ **注解式工具自动发现**：`@Tool` 方法启动时自动扫描，元数据（名称、描述、参数、分类）同步入库；
- ✅ **完整 RAG 知识库链路**：支持 `txt / md / pdf / doc / docx` 解析，`fixed / sliding / paragraph` 三种分块策略，ES `dense_vector` 向量检索；
- ✅ **Agent-知识库绑定**：一个 Agent 可绑定多个知识库，RAG 召回自动按 Agent 隔离；
- ✅ **Redis 多轮会话记忆**：`MessageWindowChatMemory` 滑动窗口 + 会话级隔离 + 历史消息查询；
- ✅ **同步 / SSE 流式双通道**：普通对话与 Agent 对话均支持流式输出；
- ✅ **Java 21 虚拟线程**：天然支撑高并发 IO 密集场景；
- ✅ **前后端分离管理后台**：Agent、工具、知识库、会话、日志全流程可视化。

---

## 三、系统架构

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Vue 3 管理后台 (Agent-A-View)               │
│        Agent 管理 │ 工具管理 │ 知识库管理 │ 对话测试 │ 调用日志        │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ HTTP / SSE (axios + EventSource)
┌──────────────────────────────▼──────────────────────────────────────┐
│                    Spring Boot 3.5 后端 (Agent-A)                    │
│                                                                      │
│  ┌──────────────────────────────────────────────────────────────┐    │
│  │  Controller 接口层（统一 Result 包装 / SSE 流式）              │    │
│  └───────────────┬──────────────────────────────┬───────────────┘    │
│                  │                              │                    │
│  ┌───────────────▼───────────────┐  ┌───────────▼───────────────┐    │
│  │  AgentRuntimeFactory          │  │  知识库 RAG 模块           │    │
│  │  · Agent 定义动态组装          │  │  · DocumentParserFactory   │    │
│  │  · 工具 Bean 反射绑定          │  │  · TextChunkerFactory      │    │
│  │  · ChatClient 组装与缓存       │  │  · ElasticsearchVectorStore│    │
│  │  · 热加载 reload              │  │  · RagAdvisor 上下文注入    │    │
│  └───────────────┬───────────────┘  └───────────┬───────────────┘    │
│                  │                              │                    │
│  ┌───────────────▼──────────────────────────────▼───────────────┐    │
│  │  ModelRegistry（多模型路由） + ToolScanner（@Tool 扫描）        │    │
│  │  ChatMemory（Redis 会话记忆） + SessionContext（会话上下文）    │    │
│  └──────────────────────────────────────────────────────────────┘    │
└───────┬──────────────────┬──────────────────────┬───────────────────┘
        │                  │                      │
  ┌─────▼─────┐      ┌─────▼─────┐          ┌─────▼─────┐
  │  MySQL    │      │   Redis   │          │ Elasticsearch│
  │  元数据/配置│      │  会话记忆  │          │  dense_vector│
  │  9 张业务表 │      │  72h TTL  │          │  kNN 向量检索 │
  └───────────┘      └───────────┘          └─────────────┘
        │                                            │
        └──────────────┬─────────────────────────────┘
                       │  OpenAI 兼容协议
          ┌────────────┼────────────┬────────────┐
      ┌───▼───┐   ┌────▼────┐  ┌────▼────┐  ┌───▼────┐
      │DeepSeek│   │  Kimi   │  │  其他   │  │BGE-M3  │
      │ 对话    │   │  对话   │  │ 对话模型 │  │Embedding│
      └────────┘   └─────────┘  └─────────┘  └────────┘
```

---

## 四、技术栈

### 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Java | 21 | 虚拟线程、Record、var 等新特性 |
| Spring Boot | 3.5.16 | 基础框架 |
| Spring AI | 1.1.2 | ChatClient / Advisor / ChatMemory 抽象 |
| Spring AI Alibaba | 1.1.2.2 | Agent 框架生态 |
| MyBatis-Plus | 3.5.17 | ORM + 代码生成器 |
| MySQL | 8.x | 元数据与业务数据持久化 |
| Redis（Lettuce） | — | 多轮会话记忆存储 |
| Elasticsearch | 8.x | `dense_vector` 向量存储 + kNN 检索 |
| Apache Tika / POI / PDFBox / CommonMark | — | 多格式文档解析 |

### 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue | 3.5.x | 前端框架 |
| Vue Router | 4.4.x | 路由（Hash 模式） |
| Element Plus | 2.9.x | UI 组件库 |
| Pinia | 2.2.x | 状态管理 |
| Axios | 1.7.x | HTTP 客户端（统一 Result 拦截） |
| Vite | 6.0.x | 构建工具 + 开发代理 |

### 大模型接入

| 模型 | 角色 | 协议 |
|------|------|------|
| DeepSeek（`deepseek-chat`） | 对话 | OpenAI 兼容 |
| Kimi / Moonshot（`kimi-k3`） | 对话 | OpenAI 兼容 |
| BGE-M3（`BAAI/bge-m3`，硅基流动） | Embedding 向量 | OpenAI 兼容 |

> 所有模型统一走 OpenAI 兼容协议，仅通过 `base-url / api-key / model` 三个字段区分，接入新模型零代码改动。

---

## 五、核心功能详解

### 1. Agent 运行时（AgentRuntimeFactory）

- 应用启动后从 `agent_def` 表加载所有启用状态的 Agent；
- 根据 `agent_tool` 绑定关系，从 Spring 容器反射定位 `@Tool` Bean；
- 组装 `ChatClient`（`System Prompt + Tools + MessageChatMemoryAdvisor + RagAdvisor`）并缓存到内存；
- 支持热加载与按模型覆盖组装（不污染默认缓存）。

### 2. 工具自动发现（ToolScanner）

- 启动时扫描 Spring 容器中所有带 `@Tool` 注解的方法；
- 自动提取方法名、描述（`@Tool(description=...)`）、参数定义（反射提取，序列化为 JSON）、分类；
- 同步策略：新工具 INSERT、已有工具 UPDATE、代码已删除工具标记禁用（不物理删除）。

### 3. 知识库 RAG（检索增强生成）

完整链路如下：

```
上传文档 → 文件类型检测 → 文档解析(多格式) → 文本分块(3策略)
        → Embedding 向量化 → 写入 ES dense_vector 索引
        → 用户提问 → RagAdvisor 召回 Top-K → 拼接到 System Prompt → 大模型回答
```

- **文档解析**（`DocumentParserFactory`，策略模式）：`PlainTextParser` / `MarkdownParser` / `PdfDocumentParser` / `WordDocumentParser`；
- **文本分块**（`TextChunkerFactory`，策略模式）：
  - `fixed`：固定长度，按句尾/换行智能截断；
  - `sliding`：滑动窗口，带重叠；
  - `paragraph`：按段落切分，长段落二次切分；
- **向量化**：调用 BGE-M3 Embedding 模型生成向量；
- **向量检索**（`ElasticsearchVectorStore`）：ES `dense_vector` + kNN 查询，支持相似度阈值过滤；
- **上下文注入**（`RagAdvisor`）：实现 Spring AI 的 `CallAdvisor` / `StreamAdvisor`，在调用大模型前自动召回知识库片段并注入。

### 4. 多模型路由（ModelRegistry）

- 启动时根据配置批量构建 `ChatModel` / `EmbeddingModel`，存入注册表；
- 按 `name`（`deepseek` / `kimi`）路由，Agent 默认模型可在 `agent_def.model_type` 指定，也可在调用时用 `modelType` 参数覆盖。

### 5. 会话记忆与隔离

- 基于 Redis 实现 `ChatMemoryRepository`，`MessageWindowChatMemory` 单会话保留最近 20 条消息，TTL 72 小时；
- 会话 id 格式：`{agentCode}_{毫秒时间戳}_{随机串}`，支持历史会话列表与历史消息查询；
- `SessionContext`（ThreadLocal）+ `SessionState`（ConcurrentHashMap）保存登录态、位置、订单等会话上下文，工具方法通过 `SessionContext.currentState()` 读取。

---

## 六、项目结构

```
Agent-W
├── Agent-Backend                          # 后端服务（Spring Boot 3）
│   ├── pom.xml
│   └── src/main/
│       ├── java/org/example/
│       │   ├── App.java                   # 启动类
│       │   ├── agent/                     # Agent 运行时核心
│       │   │   ├── AgentRuntime.java      # 已组装的可执行 Agent 实例
│       │   │   ├── AgentRuntimeFactory.java # 加载/组装/缓存/热加载
│       │   │   ├── ToolScanner.java       # @Tool 注解自动扫描注册
│       │   │   ├── SessionContext.java    # ThreadLocal 会话上下文
│       │   │   └── SessionState.java      # 会话状态（登录/位置/订单）
│       │   ├── tool/                      # 工具实现（7 个业务/运维工具）
│       │   │   ├── AuthTool / LocationTool / MenuTool / OrderTool
│       │   │   ├── RedisOperateTool / SystemTool / DateTimeTool
│       │   ├── knowledge/                 # 知识库 RAG 模块
│       │   │   ├── parse/                 # 文档解析（txt/md/pdf/docx）
│       │   │   ├── chunk/                 # 文本分块（fixed/sliding/paragraph）
│       │   │   ├── vector/                # ES 向量存储
│       │   │   └── rag/                   # RAG 召回与上下文注入
│       │   ├── config/                    # 配置（模型注册/记忆/Redis/知识库）
│       │   ├── model/                     # MyBatis-Plus 实体
│       │   ├── mapper/                    # Mapper 接口与 XML
│       │   ├── service/                   # Service 接口与实现
│       │   ├── repository/                # Redis 记忆存储
│       │   ├── common/                    # Result 统一返回
│       │   └── Controller/                # HTTP 接口层（13 个 Controller）
│       └── resources/
│           ├── application.yml            # 主配置
│           ├── application-llm.yml        # 大模型配置
│           └── init.sql                   # 建库建表 + 示例数据
│
└── Agent-Frontend                         # 前端管理后台（Vue 3）
    ├── package.json
    ├── vite.config.js                     # 端口 3000 + /api 代理到 8080
    └── src/
        ├── api/                           # axios 接口封装（10 个模块）
        ├── router/                        # 路由配置
        ├── layout/                        # 布局
        └── views/                         # 页面
            ├── Dashboard.vue              # 首页概览
            ├── AgentList.vue / AgentDetail.vue / AgentChat.vue
            ├── ToolList.vue               # 工具管理
            ├── KnowledgeList.vue / KnowledgeDetail.vue / KnowledgeDocuments.vue
            └── CallLogList.vue            # 调用日志
```

---

## 七、快速开始

### 前置条件

| 依赖 | 版本 | 说明 |
|------|------|------|
| JDK | 21 | 后端运行环境 |
| Maven | 3.8+ | 后端构建 |
| Node.js | 18+ | 前端构建 |
| MySQL | 8.x | `127.0.0.1:3306`，`root` / `123456` |
| Redis | 6+ | `127.0.0.1:6379`，密码 `123456` |
| Elasticsearch | 8.x | `http://127.0.0.1:9200`，需安装 `ik` 中文分词插件 |

### 第 1 步：初始化数据库

```sql
-- 执行 src/main/resources/init.sql，会自动建库 agent_a 并写入示例 Agent 与工具数据
```

### 第 2 步：配置大模型 API Key

在 `Agent-Backend/src/main/resources/application-llm.yml` 中配置（或通过环境变量注入）：

```yaml
app:
  models:
    chat:
      - name: deepseek
        base-url: https://api.deepseek.com
        api-key: ${DEEPSEEK_API_KEY}
        model: deepseek-chat
      - name: kimi
        base-url: https://api.moonshot.cn
        api-key: ${KIMI_API_KEY}
        model: kimi-k3
    embedding:
      - name: bge-m3
        base-url: https://api.siliconflow.cn
        api-key: ${SILICONFLOW_API_KEY}
        model: BAAI/bge-m3
```

设置环境变量：

```bash
export DEEPSEEK_API_KEY=sk-xxx
export KIMI_API_KEY=sk-xxx
export SILICONFLOW_API_KEY=sk-xxx
```

### 第 3 步：启动后端

```bash
cd Agent-Backend
mvn spring-boot:run
# 后端默认监听 8080 端口
```

### 第 4 步：启动前端

```bash
cd Agent-Frontend
npm install
npm run dev
# 前端默认监听 3000 端口，访问 http://localhost:3000
```

> 前端通过 Vite 代理将 `/api` 转发到后端 `http://127.0.0.1:8080`。

---

## 八、API 概览

| 模块 | 端点示例 | 功能 |
|------|---------|------|
| Agent 运行时 | `GET /agent/list` | 列出已加载的 Agent |
| | `GET /agent/{code}/reload` | 热加载 Agent |
| | `GET /agent/{code}/chat` | Agent 同步对话 |
| | `GET /agent/{code}/chat/stream` | Agent SSE 流式对话 |
| Agent 定义 | `GET/POST/PUT/DELETE /agentDef` | Agent 增删改查 + 克隆 |
| 工具定义 | `GET /toolDef/list`、`PUT /toolDef/{id}/toggle` | 工具列表 + 状态切换 |
| 工具绑定 | `POST /agentTool`、`DELETE /agentTool` | Agent-工具绑定/解绑 |
| 知识库 | `GET/POST/PUT/DELETE /knowledgeBase` | 知识库 CRUD |
| 文档 | `POST /knowledgeDocument/upload` | 上传文档（自动解析+向量化） |
| | `POST /knowledgeDocument/{id}/reparse` | 重新解析 |
| 分块 | `GET /knowledgeChunk/list?docId=` | 查询文档分块 |
| 知识库绑定 | `POST /agentKnowledge/bind` | Agent-知识库绑定 |
| 会话 | `GET /session/new`、`GET /session/list`、`GET /session/{id}/messages` | 会话生成/列表/历史消息 |
| 调用日志 | `GET /agentCallLog/list` | 调用日志分页 |
| Prompt 历史 | `GET /agentPromptHistory/list?agentId=` | 版本历史 |
| 模型 | `GET /model/list` | 已配置的模型路由列表 |
| 普通对话 | `GET /chat`、`GET /chat/stream` | 模型直连对话（非 Agent 模式） |

---

## 九、核心设计要点（技术亮点）

以下是本项目可深入讲述的架构设计点，也是简历与面试的加分项：

1. **配置驱动的多模型路由**：关闭 Spring AI 自带的单模型自动配置，自研 `ModelRegistry` 批量构建多模型实例，`base-url + api-key + model` 三元组统一 OpenAI 兼容协议，做到「加模型不改代码」。

2. **Agent 运行时动态组装**：Agent 定义、Prompt、工具绑定全部存数据库，`AgentRuntimeFactory` 启动时组装为 `ChatClient` 缓存，通过反射按方法名定位工具 Bean，实现运行时热加载。

3. **Advisor 责任链的 RAG 集成**：`RagAdvisor` 同时实现 Spring AI 的 `CallAdvisor` 与 `StreamAdvisor`，以「切面」方式在调用大模型前完成向量召回与上下文注入，与业务代码完全解耦。

4. **策略模式的可扩展解析/分块**：`DocumentParserFactory` 与 `TextChunkerFactory` 通过 Spring 自动注入所有实现类，新增文件格式或分块策略只需新增一个类，符合开闭原则。

5. **会话隔离与状态传递**：`ThreadLocal`（`SessionContext`）传递 `sessionId` 与 `agentCode`，`ConcurrentHashMap` 维护会话业务状态（登录态、位置、订单），保证多会话并发下的数据隔离。

6. **统一返回与前端拦截**：后端 `Result<T>` 统一 `{code, message, data}` 结构，前端 axios 响应拦截器统一解析与错误提示，SSE 流式接口单独处理。

7. **虚拟线程 + 响应式流**：Java 21 虚拟线程支撑高并发，`Flux<String>` + `TEXT_EVENT_STREAM` 实现流式输出，前后端通过 `EventSource` 对接。

---

## 十、数据库表设计

共 9 张表（详见 `Agent-Backend/src/main/resources/init.sql`）：

| 表名 | 说明 |
|------|------|
| `agent_def` | Agent 定义（编码、名称、System Prompt、模型、状态、版本） |
| `tool_def` | 工具注册表（`@Tool` 扫描同步） |
| `agent_tool` | Agent-工具绑定关系 |
| `agent_prompt_history` | Prompt 版本历史 |
| `agent_call_log` | Agent 调用日志 |
| `knowledge_base` | 知识库主表（一个知识库对应一个 ES 索引） |
| `knowledge_document` | 知识库文档表 |
| `knowledge_chunk` | 文本分块表（存元数据，向量在 ES） |
| `agent_knowledge` | Agent-知识库绑定关系 |

---

## 十一、待完善（Roadmap）

- [ ] Prompt 版本历史自动记录（修改 Agent 时自动写入版本）
- [ ] 调用日志完整记录（工具调用耗时与结果落库）
- [ ] Agent 更新后自动热加载
- [ ] 文档解析改为异步任务，避免大文件阻塞上传
- [ ] 知识库向量化支持更多 Embedding 模型与本地模型
- [ ] 前端 Agent 对话接入 SSE 流式展示

---

## 十二、许可

[MIT License](LICENSE)
