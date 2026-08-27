-- ===================================================
-- Agent-A 项目初始化 SQL
-- 包含：表结构 + 2个示例 Agent 的初始化数据
-- ===================================================

CREATE DATABASE IF NOT EXISTS agent_a;
USE agent_a;

-- ===================================================
-- 1. 工具注册表 —— 代码里有什么工具
--    ToolScanner 启动时自动扫描 @Tool 注解同步到此表，
--    下面的 INSERT 是种子数据，ToolScanner 会覆盖/补充。
-- ===================================================
CREATE TABLE tool_def (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    tool_name   VARCHAR(64)  NOT NULL UNIQUE COMMENT '对应 @Tool 方法名',
    display_name VARCHAR(64) NOT NULL COMMENT '管理后台显示名',
    category    VARCHAR(32) COMMENT '分类：业务工具/运维工具/系统工具',
    description TEXT COMMENT '工具说明（从 @Tool 注解获取）',
    parameters  JSON COMMENT '参数定义（ToolScanner 自动采集）',
    status      TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用'
) COMMENT='工具注册表';

-- ===================================================
-- 2. Agent 定义表 —— 一个 Agent 一条记录
-- ===================================================
CREATE TABLE agent_def (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    agent_code      VARCHAR(64)  NOT NULL UNIQUE COMMENT '唯一标识，如 order_helper',
    agent_name      VARCHAR(128) NOT NULL COMMENT '显示名，如蜜雪冰城点单助手',
    system_prompt   TEXT NOT NULL COMMENT 'System Prompt 系统提示词',
    model_type      VARCHAR(32) DEFAULT 'deepseek' COMMENT '模型类型：deepseek / kimi',
    status          TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用 2草稿',
    version         INT DEFAULT 1 COMMENT '版本号',
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at      DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='Agent 定义表';

-- ===================================================
-- 3. Agent-工具关联表 —— 多对多
-- ===================================================
CREATE TABLE agent_tool (
    agent_id    BIGINT COMMENT 'Agent ID',
    tool_id     BIGINT COMMENT '工具 ID',
    enabled     TINYINT DEFAULT 1 COMMENT '是否启用该绑定：1启用 0禁用',
    PRIMARY KEY (agent_id, tool_id)
) COMMENT='Agent-工具绑定关系表';

-- ===================================================
-- 4. Prompt 版本历史 —— 运营核心
-- ===================================================
CREATE TABLE agent_prompt_history (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    agent_id    BIGINT COMMENT 'Agent ID',
    version     INT COMMENT '版本号',
    prompt      TEXT COMMENT '该版本的完整 Prompt',
    change_log  VARCHAR(500) COMMENT '变更说明',
    changed_by  VARCHAR(64) COMMENT '修改人',
    changed_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '修改时间'
) COMMENT='Prompt 版本历史表';

-- ===================================================
-- 5. Agent 调用日志 —— 效果追踪
-- ===================================================
CREATE TABLE agent_call_log (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    agent_id    BIGINT COMMENT 'Agent ID',
    session_id  VARCHAR(64) COMMENT '会话 ID',
    user_input  TEXT COMMENT '用户输入',
    agent_output TEXT COMMENT 'Agent 输出',
    tool_calls  JSON COMMENT '工具调用记录及耗时',
    latency_ms  INT COMMENT '总耗时（毫秒）',
    status      VARCHAR(16) COMMENT '调用状态：success / error',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='Agent 调用日志表';

-- ===================================================
-- 6. 知识库主表 —— 一个知识库对应一个 ES 向量索引
--    向量数据库选型：Elasticsearch（dense_vector）
--    MySQL 存元数据，向量、召回检索在 ES 中完成
-- ===================================================
CREATE TABLE knowledge_base (
    id                   BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    kb_code              VARCHAR(64)  NOT NULL UNIQUE COMMENT '知识库唯一编码，用于生成 ES 索引名',
    kb_name              VARCHAR(128) NOT NULL COMMENT '知识库显示名称',
    description          TEXT COMMENT '知识库描述',
    embedding_model      VARCHAR(64) DEFAULT 'bge-large-zh' COMMENT '向量模型名称/编码',
    vector_index_name    VARCHAR(128) NOT NULL COMMENT '对应的 ES 索引名，如 kb_order_faq_chunks',
    chunk_strategy       VARCHAR(32) DEFAULT 'fixed' COMMENT '分块策略：fixed / sliding / semantic',
    chunk_size           INT DEFAULT 512 COMMENT '单个 chunk 最大字符/Token 数',
    chunk_overlap        INT DEFAULT 50 COMMENT '分块重叠长度',
    top_k                INT DEFAULT 5 COMMENT 'RAG 默认召回数量',
    similarity_threshold FLOAT DEFAULT 0.75 COMMENT '相似度阈值，低于此值不召回',
    status               TINYINT DEFAULT 1 COMMENT '状态：1启用 0禁用',
    created_at           DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='知识库主表';

-- ===================================================
-- 7. 知识库文档表 —— 一个知识库下可上传多份文档
-- ===================================================
CREATE TABLE knowledge_document (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    kb_id       BIGINT NOT NULL COMMENT '所属知识库 ID',
    doc_name    VARCHAR(255) NOT NULL COMMENT '文档原始文件名',
    file_type   VARCHAR(32) COMMENT '文件类型：pdf / word / txt / markdown',
    file_size   BIGINT COMMENT '文件大小（字节）',
    file_path   VARCHAR(500) COMMENT '文件存储路径（本地或 OSS）',
    source_url  VARCHAR(500) COMMENT '文档来源链接（可选）',
    chunk_count INT DEFAULT 0 COMMENT '已生成的 chunk 数量',
    status      TINYINT DEFAULT 0 COMMENT '解析状态：0待解析 1解析中 2已完成 9失败',
    parse_message VARCHAR(500) COMMENT '解析失败时的错误信息',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='知识库文档表';

-- ===================================================
-- 8. 知识库文本分块表 —— 每份文档切分为多个 chunk
--    向量数据存储在 ES，本表仅保存 chunk 元数据与 ES 关联 ID
-- ===================================================
CREATE TABLE knowledge_chunk (
    id           BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    doc_id       BIGINT NOT NULL COMMENT '所属文档 ID',
    kb_id        BIGINT NOT NULL COMMENT '所属知识库 ID（冗余字段，方便按知识库清理）',
    chunk_index  INT NOT NULL COMMENT '文档内 chunk 序号',
    content      TEXT NOT NULL COMMENT 'chunk 文本内容（展示/溯源用）',
    metadata     JSON COMMENT '元数据：页码、标题、段落等',
    token_count  INT COMMENT 'Token 数量估算',
    es_doc_id    VARCHAR(64) COMMENT '对应 ES 文档 ID，用于更新/删除向量',
    status       TINYINT DEFAULT 1 COMMENT '状态：1有效 0无效',
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='知识库文本分块表';

-- ===================================================
-- 9. Agent-知识库关联表 —— 一个 Agent 可绑定多个知识库
-- ===================================================
CREATE TABLE agent_knowledge (
    agent_id    BIGINT COMMENT 'Agent ID',
    kb_id       BIGINT COMMENT '知识库 ID',
    enabled     TINYINT DEFAULT 1 COMMENT '是否启用该知识库绑定：1启用 0禁用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (agent_id, kb_id)
) COMMENT='Agent-知识库绑定关系表';

-- ===================================================
-- 种子数据：初始化 2 个示例 Agent
-- ===================================================

-- ---------------------------------------------------
-- Agent 1: 蜜雪冰城点单助手 (order_helper)
-- ---------------------------------------------------
INSERT INTO agent_def (id, agent_code, agent_name, system_prompt, model_type, status, version)
VALUES (1, 'order_helper', '蜜雪冰城点单助手',
'你是「蜜雪冰城智能点单助手」，你的职责是引导用户一步步完成饮品点单，\
包括：确认位置 → 确认登录 → 推荐饮品 → 确认选择 → 下单。

## 核心行为规则

1. **状态驱动**：每个操作都有前置条件，必须按顺序执行，不能跳步：
   - 位置（getMyLocation）→ 登录（checkLoginStatus / login）→ 菜单（getRecommendedDrinks）→ 下单（placeOrder）

2. **主动检查，不要假设**：
   - 用户说"我想喝蜜雪冰城"或表达类似点单意图时，第一步永远是获取位置（getMyLocation）
   - 位置获取后，立即检查登录状态（checkLoginStatus）
   - 如果未登录，主动引导用户输入账号密码来登录（login）
   - 登录成功后，主动调用菜单（getRecommendedDrinks）展示饮品
   - 用户做出选择后（如"第2个"或"想要冰鲜柠檬水"），调用下单（placeOrder）

3. **用户输入账号密码时**：
   - 一旦用户提供了账号和密码，立即调用 login(account, password) 完成登录
   - 登录成功后，紧接着调用 getRecommendedDrinks(0,0) 展示全部饮品
   - 不要等用户再问"有什么喝的"

4. **用户说"第N个"或指定饮品名时**：
   - 直接调用 placeOrder("N") 或 placeOrder("饮品名称") 下单
   - 下单成功后，展示订单详情
   - 不要再确认"你确定要这个吗"

5. **对话风格**：
   - 热情友好，像奶茶店员一样自然交流
   - 每次回复保持简短（不超过 3 句话 + 工具结果）
   - 不要一次性输出大段文字淹没用户

6. **出错处理**：
   - 工具返回失败时，按提示引导用户操作（如"密码错了，请重试"）
   - 不要自行编造登录成功或下单成功',
'deepseek', 1, 1);

-- ---------------------------------------------------
-- Agent 2: 运维助手 (ops_helper)
-- ---------------------------------------------------
INSERT INTO agent_def (id, agent_code, agent_name, system_prompt, model_type, status, version)
VALUES (2, 'ops_helper', '系统运维助手',
'你是系统运维助手，专注于帮助用户监控和诊断 Redis、JVM、系统时间等运维信息。

## 核心行为规则

1. **按需调用工具**：
   - 用户问 Redis → 调 getRedisMonitorInfo 获取完整状态
   - 用户问 JVM/内存 → 调 getJvmInfo 获取 JVM 运行时信息
   - 用户问 CPU 核数 → 调 getCpuCores
   - 用户问时间/日期 → 调 getCurrentDateTime

2. **数据解读**：
   - 拿到工具返回的监控数据后，用通俗语言解释关键指标
   - 发现异常（如内存使用率 > 80%、缓存命中率 < 90%）时主动告警

3. **对话风格**：
   - 简洁专业，直接给出关键数据 + 一句话诊断结论
   - 不要输出冗长的解释

4. **工具列表**：
   - getRedisMonitorInfo: Redis 版本/内存/连接数/命中率/键统计
   - getJvmInfo: JVM 堆内存/CPU核数/进程运行时长
   - getCpuCores: CPU 逻辑核心数
   - getCurrentDateTime: 当前日期时间
   - daysBetween(start, end): 计算两个日期相差天数
   - addDays(date, days): 日期加减',
'deepseek', 1, 1);

-- ---------------------------------------------------
-- tool_def 种子数据（ToolScanner 启动后会补充/更新）
-- ---------------------------------------------------
INSERT INTO tool_def (id, tool_name, display_name, category, description, status) VALUES
(1,  'getMyLocation',        '定位工具',   '业务工具', '获取用户地理位置及最近蜜雪冰城门店', 1),
(2,  'checkLoginStatus',     '登录检查',   '业务工具', '检查当前会话的登录状态', 1),
(3,  'login',                '用户登录',   '业务工具', '执行账号密码登录，测试账号zhangsan/123456', 1),
(4,  'getRecommendedDrinks', '菜单推荐',   '业务工具', '获取蜜雪冰城推荐饮品列表，支持按价格区间过滤', 1),
(5,  'placeOrder',           '下单工具',   '业务工具', '按序号或名称下单指定饮品', 1),
(6,  'getLastOrder',         '订单查询',   '业务工具', '查询最近一笔订单详情', 1),
(7,  'getRedisMonitorInfo',  'Redis监控',  '运维工具', '查询Redis运行状态/内存/连接数/命中率', 1),
(8,  'getJvmInfo',           'JVM监控',    '系统工具', '查询JVM堆内存使用/CPU核数/进程运行时长', 1),
(9,  'getCpuCores',          'CPU核数',    '系统工具', '查询服务器CPU逻辑核心数', 1),
(10, 'getCurrentDateTime',   '当前时间',   '系统工具', '获取当前日期时间（含星期、时区）', 1),
(11, 'daysBetween',          '日期差计算', '系统工具', '计算两个日期(yyyy-MM-dd)之间相隔天数', 1),
(12, 'addDays',              '日期加减',   '系统工具', '指定日期加减N天后得到的日期', 1);

-- ---------------------------------------------------
-- agent_tool 关联：点单助手 ← 业务工具
-- ---------------------------------------------------
INSERT INTO agent_tool (agent_id, tool_id, enabled) VALUES
(1, 1, 1),  -- getMyLocation
(1, 2, 1),  -- checkLoginStatus
(1, 3, 1),  -- login
(1, 4, 1),  -- getRecommendedDrinks
(1, 5, 1),  -- placeOrder
(1, 6, 1);  -- getLastOrder

-- ---------------------------------------------------
-- agent_tool 关联：运维助手 ← 运维+系统工具
-- ---------------------------------------------------
INSERT INTO agent_tool (agent_id, tool_id, enabled) VALUES
(2, 7, 1),  -- getRedisMonitorInfo
(2, 8, 1),  -- getJvmInfo
(2, 9, 1),  -- getCpuCores
(2, 10, 1), -- getCurrentDateTime
(2, 11, 1), -- daysBetween
(2, 12, 1); -- addDays

-- ===================================================
-- Elasticsearch 向量索引设计说明（需在 ES 中手动创建）
-- 索引名规则：与 knowledge_base.vector_index_name 保持一致
-- 示例索引映射：
-- PUT /kb_default_chunks
-- {
--   "mappings": {
--     "properties": {
--       "kb_id":      { "type": "long" },
--       "doc_id":     { "type": "long" },
--       "chunk_id":   { "type": "long" },
--       "content":    { "type": "text", "analyzer": "ik_smart" },
--       "metadata":   { "type": "object" },
--       "embedding":  {
--         "type": "dense_vector",
--         "dims": 1024,
--         "index": true,
--         "similarity": "cosine"
--       }
--     }
--   }
-- }
-- 检索示例：使用 knn 查询，结合 similarity_threshold 过滤
-- ===================================================
