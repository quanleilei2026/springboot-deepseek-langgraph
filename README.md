# SpringBoot DeepSeek LangGraph Demo

**Author:** quanleilei
**Date:** 2026-02-28
**Tech Stack:** Vue3.x + JDK 1.8 + SpringBoot + LangGraph + LangSmith + MySQL 5.7.35
**LLM:** DeepSeek
**Maven Coordinates:** com.qll:springboot-deepseek-langgraph:1.0.0

---

## 📋 Table of Contents

- [Overview](#overview)
- [What is LangGraph?](#what-is-langgraph)
- [What is LangSmith?](#what-is-langsmith)
- [Architecture](#architecture)
- [Features Demonstrated](#features-demonstrated)
- [Prerequisites](#prerequisites)
- [Installation](#installation)
- [Configuration](#configuration)
- [Running the Application](#running-the-application)
- [API Documentation](#api-documentation)
- [Frontend Guide](#frontend-guide)
- [Code Examples](#code-examples)

---

## 🎯 Overview

This project demonstrates the integration of **LangGraph** and **LangSmith** with a SpringBoot backend and Vue3 frontend, using DeepSeek as the LLM provider. The application showcases a complete multi-agent code development workflow with full observability and tracing capabilities.

**Demo Workflow:** Researcher → Coder → Reviewer (with cyclical revision loops)

---

## 🔍 What is LangGraph?

### **LangGraph** is a library for building **stateful, multi-actor applications with LLMs**.

### Problems LangGraph Solves

1. **Complex Workflow Management**
   - Managing multi-step AI workflows with conditional logic
   - Coordinating multiple agents that need to share state
   - Implementing loops and iterations in LLM applications

2. **State Persistence**
   - Maintaining conversation context across interactions
   - Saving and resuming workflows at any point
   - Preserving intermediate results for debugging

3. **Cyclic Workflows**
   - Unlike traditional DAG-based systems, LangGraph supports **loops**
   - Enables revision cycles (e.g., Reviewer → Coder → Reviewer)
   - Conditional branching based on state

### Key LangGraph Features

#### 1. **State Persistence**
State is maintained across all nodes in the workflow. Each node can:
- Read the current state
- Update specific state variables
- Maintain context across the entire workflow

**Example from code:**
```java
// LANGGRAPH: State Persistence
public class GraphState {
    private String requirements;      // Persists across nodes
    private String researchFindings;  // Updated by Researcher
    private String generatedCode;      // Updated by Coder
    private String reviewComments;     // Updated by Reviewer
}
```

#### 2. **Cyclic Workflows**
LangGraph supports loops and iterations:

```java
// LANGGRAPH: Cyclic Workflows
public String determineNextNode(GraphState state) {
    if (state.needsRevision()) {
        return "Coder";  // Loop back to Coder
    }
    return "END";        // Or complete workflow
}
```

**Demonstrated in:** Reviewer → Coder loops for code revisions

#### 3. **Multi-Agent Coordination**
Multiple agents work together with shared state:

```java
// LANGGRAPH: Multi-agent Coordination
ResearcherNode → researches APIs and libraries
CoderNode      → writes code based on research
ReviewerNode   → reviews and requests revisions
```

#### 4. **Checkpointing**
Save workflow state at any point for resumption:

```java
// LANGGRAPH: Checkpointing
private void saveCheckpoint(GraphState state, String checkpointName) {
    // Save state to database
    // Enables pause/resume functionality
}
```

**Frontend Button:** "Pause & Resume Development"

---

## 🔍 What is LangSmith?

### **LangSmith** is a development platform for **building, testing, and debugging LLM applications**.

### Problems LangSmith Solves

1. **Debugging Complexity**
   - Tracing execution flows through complex LLM chains
   - Identifying bottlenecks and failures
   - Understanding intermediate results

2. **Testing & Evaluation**
   - Running systematic test suites
   - Comparing different prompts and models
   - Measuring performance objectively

3. **Production Monitoring**
   - Tracking costs and token usage
   - Monitoring latency and performance
   - Observing application behavior in real-time

### Key LangSmith Features

#### 1. **End-to-end Tracing**
Every step of the workflow is traced:

```java
// LANGSMITH: Tracing
public void recordTrace(String runId, String nodeName,
                       Map<String, Object> inputs,
                       Map<String, Object> outputs,
                       int tokenUsage, long latency) {
    // Records: inputs, outputs, tokens, timing
}
```

**Demonstrated in:** Every node execution and LLM call

#### 2. **Debugging View**
Inspect inputs and outputs at each step:

```java
// LANGSMITH: Debugging
public TraceEvent getTraceDetail(String runId) {
    // Returns detailed trace with:
    // - Node inputs and outputs
    // - Token usage per node
    // - Execution timing
}
```

**Frontend Button:** "Trace Detail View"

#### 3. **Evaluation Framework**
Evaluate workflows against different criteria:

```java
// LANGSMITH: Evaluation
public EvaluationResult evaluateRun(String runId, String evaluationType) {
    // Evaluates: EFFICIENCY, QUALITY, PERFORMANCE
    // Returns: score, passed/failed, feedback
}
```

**Evaluation Types:**
- **EFFICIENCY:** Based on iteration count
- **QUALITY:** Based on revision count (fewer = better)
- **PERFORMANCE:** Based on execution time

**Frontend Button:** "Run Development Evaluation"

#### 4. **Performance Metrics**
Track costs and performance:

```java
// LANGSMITH: Performance Metrics
public PerformanceMetrics getPerformanceMetrics(String runId) {
    // Returns: total tokens, latency, cost, iterations, revisions
}
```

**Frontend Button:** "Performance Metrics"

---

## 🏗️ Architecture

### Backend (SpringBoot + JDK 1.8)

```
src/main/java/com/qll/
├── config/
│   ├── ApiConfig.java              # Centralized API management
│   ├── LangGraphConfig.java        # LangGraph configuration
│   └── LangSmithConfig.java        # LangSmith configuration
├── controller/
│   └── LangGraphController.java    # REST API endpoints
├── service/
│   └── LangSmithService.java       # Tracing and observability
├── model/
│   ├── GraphState.java             # LANGGRAPH: State model
│   ├── AgentNode.java              # LANGGRAPH: Node model
│   └── entities/                   # Database entities
├── langgraph/
│   ├── nodes/
│   │   ├── ResearcherNode.java     # Research agent
│   │   ├── CoderNode.java          # Coding agent
│   │   └── ReviewerNode.java       # Review agent
│   └── workflows/
│       └── MultiAgentWorkflow.java # Workflow orchestration
└── SpringbootDeepseekLanggraphApplication.java
```

### Frontend (Vue3 + Element Plus)

```
frontend/src/
├── App.vue                         # Main application
├── main.js                         # Application entry
├── api/
│   └── langgraph.js                # API service
└── components/
    ├── LangGraphPanel.vue          # LANGGRAPH features
    └── LangSmithPanel.vue          # LANGSMITH features
```

### Database (MySQL 5.7.35)

- `threads` - Thread information for state persistence
- `thread_states` - State snapshots for checkpointing
- `checkpoints` - Checkpoint data for workflow resumption
- `runs` - Workflow runs for tracing
- `traces` - Detailed trace events
- `evaluations` - Evaluation results
- `performance_metrics` - Performance data

---

## ✨ Features Demonstrated

### LANGGRAPH Features

| Feature | Description | Frontend Button |
|----------|-------------|-----------------|
| **State Persistence** | State maintained across all nodes | View Development State |
| **Cyclic Workflows** | Reviewer → Coder loops for revisions | Request Revision |
| **Multi-agent Coordination** | Researcher, Coder, Reviewer collaboration | Start Development Cycle |
| **Checkpointing** | Save/resume workflow at any point | Pause & Resume Development |
| **Workflow Visualization** | Visual representation of workflow structure | View Workflow Graph |

### LANGSMITH Features

| Feature | Description | Frontend Button |
|----------|-------------|-----------------|
| **End-to-end Tracing** | Every step traced with full context | View Development Traces |
| **Debugging View** | Inspect inputs/outputs at each step | Trace Detail View |
| **Evaluation Framework** | Test different prompts/configurations | Run Development Evaluation |
| **Performance Metrics** | Token usage, latency, costs | Performance Metrics |
| **Comparative Analysis** | Compare multiple runs side-by-side | Compare Development Cycles |

---

## 📦 Prerequisites

1. **Java Development Kit (JDK) 1.8**
   ```bash
   java -version  # Should show 1.8.x
   ```

2. **Maven 3.6+**
   ```bash
   mvn -version
   ```

3. **MySQL 5.7.35**
   ```bash
   mysql --version  # Should show 5.7.35
   ```

4. **Node.js 16+** (for frontend)
   ```bash
   node -v
   npm -v
   ```

5. **DeepSeek API Key**
   - Sign up at: https://platform.deepseek.com/
   - Get your API key from the dashboard

6. **LangSmith API Key** (optional, for advanced tracing)
   - Sign up at: https://smith.langchain.com/
   - Create a project and get API key

---

## 🚀 Installation

### 1. Clone the Repository

```bash
git clone https://github.com/quanleilei2026/springboot-deepseek-langgraph.git
cd springboot-deepseek-langgraph
```

### 2. Database Setup

```bash
# Login to MySQL
mysql -u root -p

# Execute the schema
source src/main/resources/db/schema.sql
```

Or use command line:
```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

### 3. Build Backend

```bash
mvn clean install
```

### 4. Build Frontend

```bash
cd frontend
npm install
npm run build
cd ..
```

---

## ⚙️ Configuration

### 1. API Keys Configuration

Create `application-override.yml` in `src/main/resources/`:

```yaml
api:
  deepseek:
    url: https://api.deepseek.com/v1
    key: YOUR_DEEPSEEK_API_KEY  # Required
    model: deepseek-chat
    timeout: 60000
    max-tokens: 4000
    temperature: 0.7

  langsmith:
    url: https://api.smith.langchain.com
    key: YOUR_LANGSMITH_API_KEY  # Optional
    project: langgraph-demo
    tracing-enabled: true

spring:
  datasource:
    password: YOUR_MYSQL_PASSWORD  # Update if needed
```

### 2. Environment Variables (Alternative)

```bash
export DEEPSEEK_API_KEY=your_deepseek_key
export LANGSMITH_API_KEY=your_langsmith_key
export DB_PASSWORD=your_mysql_password
```

---

## 🏃 Running the Application

### Start Backend

```bash
mvn spring-boot:run
```

Backend will start on `http://localhost:8080/api`

### Start Frontend (Development Mode)

```bash
cd frontend
npm run dev
```

Frontend will start on `http://localhost:3000`

### Access the Application

Open your browser and navigate to:
```
http://localhost:3000
```

You should see the LangGraph & LangSmith Demo interface with two panels:
- **LangGraph Panel** (left) - Workflow controls
- **LangSmith Panel** (right) - Observability controls

---

## 📚 API Documentation

### Workflow Endpoints

#### Start Development Cycle
```http
POST /api/workflow/start
Content-Type: application/json

{
  "requirements": "Create a REST API endpoint",
  "threadId": "optional-thread-id"
}
```

#### View Development State
```http
GET /api/workflow/state/{threadId}
```

#### Resume from Checkpoint
```http
POST /api/workflow/resume
Content-Type: application/json

{
  "threadId": "thread-id",
  "checkpointName": "Coder_BEFORE",
  "updatedRequirements": "optional-new-requirements"
}
```

#### Request Revision
```http
POST /api/workflow/revision
Content-Type: application/json

{
  "threadId": "thread-id",
  "revisionComments": "Fix the error handling"
}
```

#### Reset Workflow
```http
DELETE /api/workflow/reset/{threadId}
```

#### View Workflow Graph
```http
GET /api/workflow/graph
```

### Tracing Endpoints

#### View All Traces
```http
GET /api/workflow/traces
```

#### Get Trace Detail
```http
GET /api/workflow/traces/{runId}
```

#### Run Evaluation
```http
POST /api/workflow/evaluate
Content-Type: application/json

{
  "runId": "run-id",
  "evaluationType": "QUALITY"  // EFFICIENCY | QUALITY | PERFORMANCE
}
```

#### Get Performance Metrics
```http
GET /api/workflow/metrics/{runId}
```

#### Compare Runs
```http
GET /api/workflow/compare?runIds=run1,run2,run3
```

---

## 🖥️ Frontend Guide

### LangGraph Panel Buttons

1. **Start Development Cycle**
   - Triggers the Researcher → Coder → Reviewer workflow
   - Demonstrates: Multi-agent coordination, state persistence

2. **View Development State**
   - Shows current workflow state
   - Displays: requirements, research findings, code, review comments

3. **Pause & Resume Development**
   - Prompts for checkpoint name
   - Demonstrates: Checkpointing, state persistence

4. **Request Revision**
   - Manually triggers a Reviewer → Coder cycle
   - Demonstrates: Cyclic workflows, conditional branching

5. **Reset Development**
   - Clears all workflow state
   - Demonstrates: State management

6. **View Workflow Graph**
   - Shows the workflow structure
   - Displays: nodes, edges, conditional branches

### LangSmith Panel Buttons

1. **View Development Traces**
   - Lists all recent workflow executions
   - Shows: run ID, status, iterations, revisions

2. **Trace Detail View**
   - Shows detailed execution trace
   - Displays: node executions, tokens, timing, inputs/outputs

3. **Run Development Evaluation**
   - Evaluates a run against criteria
   - Options: EFFICIENCY, QUALITY, PERFORMANCE

4. **Performance Metrics**
   - Shows performance metrics for a run
   - Displays: tokens, latency, cost, iterations, revisions

5. **Compare Development Cycles**
   - Compares multiple runs side-by-side
   - Demonstrates: Comparative analysis

---

## 💻 Code Examples

### LANGGRAPH: Multi-Agent Workflow

```java
// LANGGRAPH: Multi-agent Coordination
public GraphState executeWorkflow(String requirements, String threadId) {
    // Initialize state
    GraphState state = GraphState.initialState(threadId, runId, requirements);

    // Execute nodes in sequence
    state = researcherNode.execute(state, requirements);     // Research
    state = coderNode.execute(state, "");                    // Code
    state = reviewerNode.execute(state, "");                 // Review

    // LANGGRAPH: Conditional Branching
    if (state.needsRevision()) {
        state = coderNode.execute(state, "");                // Revise
        state = reviewerNode.execute(state, "");             // Re-review
    }

    return state;
}
```

### LANGGRAPH: State Persistence

```java
// LANGGRAPH: State Persistence
public class GraphState {
    private String requirements;      // Shared across nodes
    private String researchFindings;  // Updated by Researcher
    private String generatedCode;      // Updated by Coder
    private String reviewComments;     // Updated by Reviewer

    // LANGGRAPH: State update
    public void updateState(String nodeName, Map<String, Object> updates) {
        this.currentNode = nodeName;
        // Apply updates
        this.lastUpdated = System.currentTimeMillis();
    }
}
```

### LANGGRAPH: Cyclic Workflows

```java
// LANGGRAPH: Cyclic Workflows
@Override
public String determineNextNode(GraphState state) {
    // LANGGRAPH: Conditional Branching
    if (state.needsRevision()) {
        return "Coder";  // Loop back to Coder for revision
    }
    return "END";        // Or complete workflow
}
```

### LANGGRAPH: Checkpointing

```java
// LANGGRAPH: Checkpointing
private void saveCheckpoint(GraphState state, String checkpointName) {
    // Save state to database
    // Enables pause/resume functionality
    // Demonstrates: State persistence, checkpointing
}
```

### LANGSMITH: Tracing

```java
// LANGSMITH: End-to-end Tracing
public void startRun(String runId, String threadId, String runType,
                    Map<String, Object> inputs) {
    // LANGSMITH: Start tracing workflow
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
    // LANGSMITH: Trace each node execution
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

### LANGSMITH: Evaluation

```java
// LANGSMITH: Evaluation Framework
public EvaluationResult evaluateRun(String runId, String evaluationType) {
    RunTrace run = runs.get(runId);

    // Calculate score based on evaluation type
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

### LANGSMITH: Performance Metrics

```java
// LANGSMITH: Performance Metrics
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

## 🔑 Centralized API Management

**All API URLs and Keys are centrally managed** in `application.yml`:

```yaml
# ============================================================================
# CENTRALIZED API MANAGEMENT (Requirement 9)
# All API URLs and Keys are managed centrally for easy maintenance
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

This centralized approach makes it easy to:
- Update API URLs without code changes
- Switch between different LLM providers
- Manage keys securely via environment variables
- Maintain consistent configuration across environments

---

## 🛠️ Troubleshooting

### Common Issues

1. **API Key Not Found**
   ```
   Error: DeepSeek API key not configured
   Solution: Set DEEPSEEK_API_KEY in application-override.yml or environment
   ```

2. **Database Connection Failed**
   ```
   Error: Could not connect to MySQL
   Solution: Verify MySQL is running and credentials in application.yml are correct
   ```

3. **Port Already in Use**
   ```
   Error: Port 8080 is already in use
   Solution: Change server.port in application.yml or stop the conflicting process
   ```

4. **Frontend API Errors**
   ```
   Error: Network Error
   Solution: Ensure backend is running on port 8080 and Vite proxy is configured
   ```

---

## 📝 License

This project is for demonstration purposes.

---

## 🤝 Contributing

This is a demo project. For questions or issues, please contact the author.

---

## 📧 Contact

**Author:** quanleilei
**Date:** 2026-02-28

---

## 🎓 Additional Resources

- [LangGraph Documentation](https://langchain-ai.github.io/langgraph/)
- [LangSmith Documentation](https://docs.smith.langchain.com/)
- [DeepSeek API Documentation](https://platform.deepseek.com/docs)
- [SpringBoot Documentation](https://spring.io/projects/spring-boot)
- [Vue3 Documentation](https://vuejs.org/)

---

**Built with ❤️ using LangGraph, LangSmith, and DeepSeek**
