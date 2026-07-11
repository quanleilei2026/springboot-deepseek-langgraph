# SpringBoot DeepSeek LangGraph 演示项目

**作者：** quanleilei
**日期：** 2026-02-28
**技术栈：** Vue3.x + JDK 1.8 + SpringBoot + LangGraph + LangSmith + MySQL 5.7.35
**大语言模型：** DeepSeek
**Maven 坐标：** com.qll:springboot-deepseek-langgraph:1.0.0

---

## 📋 目录

- [项目概述](#项目概述)
- [什么是 LangGraph？](#什么是-langgraph)
- [什么是 LangSmith？](#什么是-langsmith)
- [系统架构](#系统架构)
- [演示功能](#演示功能)
- [前置要求](#前置要求)
- [安装步骤](#安装步骤)
- [配置说明](#配置说明)
- [运行应用](#运行应用)
- [API 文档](#api-文档)
- [前端指南](#前端指南)
- [代码示例](#代码示例)

---

## 🎯 项目概述

本项目演示了 **LangGraph** 和 **LangSmith** 与 SpringBoot 后端和 Vue3 前端的集成，使用 DeepSeek 作为大语言模型提供商。应用展示了完整的多智能体代码开发工作流，具备全面的可观测性和追踪能力。

**演示工作流：** 研究员 → 编码员 → 审查员（支持循环修改流程）

---

## 🔍 什么是 LangGraph？

### **LangGraph** 是一个用于构建**有状态、多智能体大语言模型应用**的库。

### LangGraph 解决的问题

1. **复杂工作流管理**
   - 管理具有条件逻辑的多步骤 AI 工作流
   - 协调需要共享状态的多个智能体
   - 在大语言模型应用中实现循环和迭代

2. **状态持久化**
   - 在交互之间维护对话上下文
   - 在任意点保存和恢复工作流
   - 保存中间结果用于调试

3. **循环工作流**
   - 与传统的基于 DAG 的系统不同，LangGraph 支持**循环**
   - 支持修改循环（例如：审查员 → 编码员 → 审查员）
   - 基于状态的条件分支

### LangGraph 核心功能

#### 1. **状态持久化**
状态在工作流的所有节点间维护。每个节点可以：
- 读取当前状态
- 更新特定的状态变量
- 在整个工作流中维护上下文

**代码示例：**
```java
// LANGGRAPH: 状态持久化
public class GraphState {
    private String requirements;      // 在节点间持久化
    private String researchFindings;  // 由研究员更新
    private String generatedCode;      // 由编码员更新
    private String reviewComments;     // 由审查员更新
}
```

#### 2. **循环工作流**
LangGraph 支持循环和迭代：

```java
// LANGGRAPH: 循环工作流
public String determineNextNode(GraphState state) {
    if (state.needsRevision()) {
        return "Coder";  // 循环回编码员
    }
    return "END";        // 或完成工作流
}
```

**演示：** 审查员 → 编码员代码修改循环

#### 3. **多智能体协调**
多个智能体通过共享状态协同工作：

```java
// LANGGRAPH: 多智能体协调
ResearcherNode → 研究 API 和库
CoderNode      → 基于研究编写代码
ReviewerNode   → 审查代码并请求修改
```

#### 4. **检查点**
在任意点保存工作流状态以便恢复：

```java
// LANGGRAPH: 检查点
private void saveCheckpoint(GraphState state, String checkpointName) {
    // 保存状态到数据库
    // 启用暂停/恢复功能
}
```

**前端按钮：** "暂停 & 恢复开发"

---

## 🔍 什么是 LangSmith？

### **LangSmith** 是一个用于**构建、测试和调试大语言模型应用**的开发平台。

### LangSmith 解决的问题

1. **调试复杂性**
   - 追踪复杂大语言模型链中的执行流程
   - 识别瓶颈和故障点
   - 理解中间结果

2. **测试和评估**
   - 运行系统测试套件
   - 比较不同的提示和模型
   - 客观测量性能

3. **生产监控**
   - 跟踪成本和 token 使用量
   - 监控延迟和性能
   - 实时观察应用行为

### LangSmith 核心功能

#### 1. **端到端追踪**
工作流的每一步都被追踪：

```java
// LANGSMITH: 追踪
public void recordTrace(String runId, String nodeName,
                       Map<String, Object> inputs,
                       Map<String, Object> outputs,
                       int tokenUsage, long latency) {
    // 记录：输入、输出、tokens、时间
}
```

**演示：** 每个节点执行和大语言模型调用

#### 2. **调试视图**
检查每一步的输入和输出：

```java
// LANGSMITH: 调试
public TraceEvent getTraceDetail(String runId) {
    // 返回详细追踪信息：
    // - 节点输入和输出
    // - 每个节点的 token 使用量
    // - 执行时间
}
```

**前端按钮：** "追踪详情视图"

#### 3. **评估框架**
根据不同标准评估工作流：

```java
// LANGSMITH: 评估
public EvaluationResult evaluateRun(String runId, String evaluationType) {
    // 评估：效率、质量、性能
    // 返回：分数、通过/失败、反馈
}
```

**评估类型：**
- **效率 (EFFICIENCY)：** 基于迭代次数
- **质量 (QUALITY)：** 基于修改次数（越少越好）
- **性能 (PERFORMANCE)：** 基于执行时间

**前端按钮：** "运行开发评估"

#### 4. **性能指标**
跟踪成本和性能：

```java
// LANGSMITH: 性能指标
public PerformanceMetrics getPerformanceMetrics(String runId) {
    // 返回：总 tokens、延迟、成本、迭代次数、修改次数
}
```

**前端按钮：** "性能指标"

---

## 🏗️ 系统架构

### 后端 (SpringBoot + JDK 1.8)

```
src/main/java/com/qll/
├── config/
│   ├── ApiConfig.java              # 集中式 API 管理
│   ├── LangGraphConfig.java        # LangGraph 配置
│   └── LangSmithConfig.java        # LangSmith 配置
├── controller/
│   └── LangGraphController.java    # REST API 端点
├── service/
│   └── LangSmithService.java       # 追踪和可观测性
├── model/
│   ├── GraphState.java             # LANGGRAPH: 状态模型
│   ├── AgentNode.java              # LANGGRAPH: 节点模型
│   └── entities/                   # 数据库实体
├── langgraph/
│   ├── nodes/
│   │   ├── ResearcherNode.java     # 研究智能体
│   │   ├── CoderNode.java          # 编码智能体
│   │   └── ReviewerNode.java       # 审查智能体
│   └── workflows/
│       └── MultiAgentWorkflow.java # 工作流编排
└── SpringbootDeepseekLanggraphApplication.java
```

### 前端 (Vue3 + Element Plus)

```
frontend/src/
├── App.vue                         # 主应用
├── main.js                         # 应用入口
├── api/
│   └── langgraph.js                # API 服务
└── components/
    ├── LangGraphPanel.vue          # LANGGRAPH 功能
    └── LangSmithPanel.vue          # LANGSMITH 功能
```

### 数据库 (MySQL 5.7.35)

- `threads` - 状态持久化的线程信息
- `thread_states` - 检查点的状态快照
- `checkpoints` - 工作流恢复的检查点数据
- `runs` - 追踪的工作流运行
- `traces` - 详细追踪事件
- `evaluations` - 评估结果
- `performance_metrics` - 性能数据

---

## ✨ 演示功能

### LANGGRAPH 功能

| 功能 | 描述 | 前端按钮 |
|----------|-------------|-----------------|
| **状态持久化** | 状态在所有节点间维护 | 查看开发状态 |
| **循环工作流** | 审查员 → 编码员修改循环 | 请求修改 |
| **多智能体协调** | 研究员、编码员、审查员协作 | 启动开发周期 |
| **检查点** | 在任意点保存/恢复工作流 | 暂停 & 恢复开发 |
| **工作流可视化** | 工作流结构的可视化表示 | 查看工作流图 |

### LANGSMITH 功能

| 功能 | 描述 | 前端按钮 |
|----------|-------------|-----------------|
| **端到端追踪** | 每一步都有完整上下文追踪 | 查看开发追踪 |
| **调试视图** | 检查每一步的输入/输出 | 追踪详情视图 |
| **评估框架** | 测试不同的提示/配置 | 运行开发评估 |
| **性能指标** | Token 使用量、延迟、成本 | 性能指标 |
| **对比分析** | 并排比较多次运行 | 比较开发周期 |

---

## 📦 前置要求

1. **Java 开发工具包 (JDK) 1.8**
   ```bash
   java -version  # 应该显示 1.8.x
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL 5.7.35**
   ```bash
   mysql --version  # 应该显示 5.7.35
   ```

4. **Node.js 16+** (用于前端)
   ```bash
   node -v
   npm -v
   ```

5. **DeepSeek API 密钥**
   - 注册地址：https://platform.deepseek.com/
   - 从控制面板获取您的 API 密钥

6. **LangSmith API 密钥** (可选，用于高级追踪)
   - 注册地址：https://smith.langchain.com/
   - 创建项目并获取 API 密钥

---

## 🚀 安装步骤

### 1. 克隆仓库

```bash
git clone https://github.com/quanleilei2026/springboot-deepseek-langgraph.git
cd springboot-deepseek-langgraph
```

### 2. 数据库设置

```bash
# 登录 MySQL
mysql -u root -p

# 执行模式
source src/main/resources/db/schema.sql
```

或使用命令行：
```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. 构建后端

```bash
mvn clean install
```

### 4. 构建前端

```bash
cd frontend
npm install
npm run build
cd ..
```

---

## ⚙️ 配置说明

### 1. API 密钥配置

在 `src/main/resources/` 中创建 `application-override.yml`：

```yaml
api:
  deepseek:
    url: https://api.deepseek.com/v1
    key: YOUR_DEEPSEEK_API_KEY  # 必需
    model: deepseek-chat
    timeout: 60000
    max-tokens: 4000
    temperature: 0.7

  langsmith:
    url: https://api.smith.langchain.com
    key: YOUR_LANGSMITH_API_KEY  # 可选
    project: langgraph-demo
    tracing-enabled: true

spring:
  datasource:
    password: YOUR_MYSQL_PASSWORD  # 如需要则更新
```

### 2. 环境变量 (替代方法)

```bash
export DEEPSEEK_API_KEY=your_deepseek_key
export LANGSMITH_API_KEY=your_langsmith_key
export DB_PASSWORD=your_mysql_password
```

---

## 🏃 运行应用

### 启动后端

```bash
mvn spring-boot:run
```

后端将启动在 `http://localhost:8080/api`

### 启动前端 (开发模式)

```bash
cd frontend
npm run dev
```

前端将启动在 `http://localhost:3000`

### 访问应用

打开浏览器并导航到：
```
http://localhost:3000
```

您应该看到 LangGraph & LangSmith 演示界面，包含两个面板：
- **LangGraph 面板** (左侧) - 工作流控制
- **LangSmith 面板** (右侧) - 可观测性控制

---

## 📚 API 文档

### 工作流端点

#### 启动开发周期
```http
POST /api/workflow/start
Content-Type: application/json

{
  "requirements": "创建一个 REST API 端点",
  "threadId": "optional-thread-id"
}
```

#### 查看开发状态
```http
GET /api/workflow/state/{threadId}
```

#### 从检查点恢复
```http
POST /api/workflow/resume
Content-Type: application/json

{
  "threadId": "thread-id",
  "checkpointName": "Coder_BEFORE",
  "updatedRequirements": "optional-new-requirements"
}
```

#### 请求修改
```http
POST /api/workflow/revision
Content-Type: application/json

{
  "threadId": "thread-id",
  "revisionComments": "修复错误处理"
}
```

#### 重置工作流
```http
DELETE /api/workflow/reset/{threadId}
```

#### 查看工作流图
```http
GET /api/workflow/graph
```

### 追踪端点

#### 查看所有追踪
```http
GET /api/workflow/traces
```

#### 获取追踪详情
```http
GET /api/workflow/traces/{runId}
```

#### 运行评估
```http
POST /api/workflow/evaluate
Content-Type: application/json

{
  "runId": "run-id",
  "evaluationType": "QUALITY"  // EFFICIENCY | QUALITY | PERFORMANCE
}
```

#### 获取性能指标
```http
GET /api/workflow/metrics/{runId}
```

#### 比较运行
```http
GET /api/workflow/compare?runIds=run1,run2,run3
```

---

## 🖥️ 前端指南

### LangGraph 面板按钮

1. **启动开发周期**
   - 触发研究员 → 编码员 → 审查员工作流
   - 演示：多智能体协调、状态持久化

2. **查看开发状态**
   - 显示当前工作流状态
   - 展示：需求、研究发现、代码、审查意见

3. **暂停 & 恢复开发**
   - 提示输入检查点名称
   - 演示：检查点、状态持久化

4. **请求修改**
   - 手动触发审查员 → 编码员循环
   - 演示：循环工作流、条件分支

5. **重置开发**
   - 清除所有工作流状态
   - 演示：状态管理

6. **查看工作流图**
   - 显示工作流结构
   - 展示：节点、边、条件分支

### LangSmith 面板按钮

1. **查看开发追踪**
   - 列出所有最近的工作流执行
   - 显示：运行 ID、状态、迭代次数、修改次数

2. **追踪详情视图**
   - 显示详细执行追踪
   - 展示：节点执行、tokens、时间、输入/输出

3. **运行开发评估**
   - 根据标准评估运行
   - 选项：效率、质量、性能

4. **性能指标**
   - 显示运行的性能指标
   - 展示：tokens、延迟、成本、迭代次数、修改次数

5. **比较开发周期**
   - 并排比较多次运行
   - 演示：对比分析

---

## 💻 代码示例

### LANGGRAPH: 多智能体工作流

```java
// LANGGRAPH: 多智能体协调
public GraphState executeWorkflow(String requirements, String threadId) {
    // 初始化状态
    GraphState state = GraphState.initialState(threadId, runId, requirements);

    // 按顺序执行节点
    state = researcherNode.execute(state, requirements);     // 研究
    state = coderNode.execute(state, "");                    // 编码
    state = reviewerNode.execute(state, "");                 // 审查

    // LANGGRAPH: 条件分支
    if (state.needsRevision()) {
        state = coderNode.execute(state, "");                // 修改
        state = reviewerNode.execute(state, "");             // 重新审查
    }

    return state;
}
```

### LANGGRAPH: 状态持久化

```java
// LANGGRAPH: 状态持久化
public class GraphState {
    private String requirements;      // 在节点间共享
    private String researchFindings;  // 由研究员更新
    private String generatedCode;      // 由编码员更新
    private String reviewComments;     // 由审查员更新

    // LANGGRAPH: 状态更新
    public void updateState(String nodeName, Map<String, Object> updates) {
        this.currentNode = nodeName;
        // 应用更新
        this.lastUpdated = System.currentTimeMillis();
    }
}
```

### LANGGRAPH: 循环工作流

```java
// LANGGRAPH: 循环工作流
@Override
public String determineNextNode(GraphState state) {
    // LANGGRAPH: 条件分支
    if (state.needsRevision()) {
        return "Coder";  // 循环回编码员进行修改
    }
    return "END";        // 或完成工作流
}
```

### LANGGRAPH: 检查点

```java
// LANGGRAPH: 检查点
private void saveCheckpoint(GraphState state, String checkpointName) {
    // 保存状态到数据库
    // 启用暂停/恢复功能
    // 演示：状态持久化、检查点
}
```

### LANGSMITH: 追踪

```java
// LANGSMITH: 端到端追踪
public void startRun(String runId, String threadId, String runType,
                    Map<String, Object> inputs) {
    // LANGSMITH: 开始追踪工作流
    RunTrace run = RunTrace.builder()
        .id(runId)
        .threadId(threadId)
        .runType(runType)
        .inputs(inputs)
        .build();

    runs.put(runId, run);
}

public void recordTrace(String runId, String nodeName,
                       Map<String, Object> inputs,
                       Map<String, Object> outputs,
                       int tokenUsage, long latency) {
    // LANGSMITH: 追踪每个节点执行
    TraceEvent trace = TraceEvent.builder()
        .nodeName(nodeName)
        .inputs(inputs)
        .outputs(outputs)
        .tokenUsage(tokenUsage)
        .latencyMs(latency)
        .build();

    traces.get(runId).add(trace);
}
```

### LANGSMITH: 评估

```java
// LANGSMITH: 评估框架
public EvaluationResult evaluateRun(String runId, String evaluationType) {
    RunTrace run = runs.get(runId);

    // 根据评估类型计算分数
    double score = calculateScore(run, evaluationType);
    boolean passed = score >= 70.0;
    String feedback = generateFeedback(run, evaluationType, score);

    return EvaluationResult.builder()
        .runId(runId)
        .evaluationType(evaluationType)
        .score(score)
        .passed(passed)
        .feedback(feedback)
        .build();
}
```

### LANGSMITH: 性能指标

```java
// LANGSMITH: 性能指标
public PerformanceMetrics getPerformanceMetrics(String runId) {
    RunTrace run = runs.get(runId);

    return PerformanceMetrics.builder()
        .runId(runId)
        .totalTokens(run.getTotalTokens())
        .totalLatencyMs(run.getTotalDuration())
        .totalCost(calculateCost(run.getTotalTokens()))
        .iterationCount(run.getIterationCount())
        .revisionCount(run.getRevisionCount())
        .build();
}
```

---

## 🔑 集中式 API 管理

**所有 API URL 和密钥都在 `application.yml` 中集中管理：**

```yaml
# ============================================================================
# 集中式 API 管理 (需求 9)
# 所有 API URL 和密钥都集中管理，便于维护
# ============================================================================
api:
  deepseek:
    url: ${DEEPSEEK_API_URL:https://api.deepseek.com/v1}
    key: ${DEEPSEEK_API_KEY}
    model: ${DEEPSEEK_MODEL:deepseek-chat}

  langsmith:
    url: ${LANGSMITH_API_URL:https://api.smith.langchain.com}
    key: ${LANGSMITH_API_KEY}
    project: ${LANGSMITH_PROJECT:langgraph-demo}
```

这种集中式方法使得：
- 无需更改代码即可更新 API URL
- 在不同大语言模型提供商之间切换
- 通过环境变量安全管理密钥
- 在不同环境间保持配置一致性

---

## 🛠️ 故障排除

### 常见问题

1. **API 密钥未找到**
   ```
   错误：DeepSeek API 密钥未配置
   解决方案：在 application-override.yml 或环境中设置 DEEPSEEK_API_KEY
   ```

2. **数据库连接失败**
   ```
   错误：无法连接到 MySQL
   解决方案：验证 MySQL 正在运行，且 application.yml 中的凭据正确
   ```

3. **端口已被占用**
   ```
   错误：端口 8080 已被占用
   解决方案：在 application.yml 中更改 server.port 或停止冲突进程
   ```

4. **前端 API 错误**
   ```
   错误：网络错误
   解决方案：确保后端在端口 8080 上运行，且 Vite 代理配置正确
   ```

---

## 📝 许可证

本项目用于演示目的。

---

## 🤝 贡献

这是一个演示项目。如有问题或疑问，请联系作者。

---

## 📧 联系方式

**作者：** quanleilei
**日期：** 2026-02-28

---

## 🎓 其他资源

- [LangGraph 文档](https://langchain-ai.github.io/langgraph/)
- [LangSmith 文档](https://docs.smith.langchain.com/)
- [DeepSeek API 文档](https://platform.deepseek.com/docs)
- [SpringBoot 文档](https://spring.io/projects/spring-boot)
- [Vue3 文档](https://vuejs.org/)

---

**使用 LangGraph、LangSmith 和 DeepSeek 构建 ❤️**
