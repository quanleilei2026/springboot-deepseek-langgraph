package com.qll.service;

import com.qll.config.ApiConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LANGSMITH: 追踪和可观测性服务
 * LangSmith Tracing and Observability Service
 *
 * 核心LANGSMITH功能演示:
 * - End-to-end Tracing (端到端追踪): 追踪工作流执行的每个步骤
 * - Debugging (调试视图): 捕获输入、输出和中间结果
 * - Performance Metrics (性能指标): 跟踪token使用量、延迟和成本
 * - Evaluation Framework (评估框架): 评估工作流结果
 *
 * LangSmith数据可见性配置说明:
 * 1. 需要在application.yml中配置有效的LANGSMITH_API_KEY
 * 2. 项目名称(LANGSMITH_PROJECT)必须在LangSmith平台中存在
 * 3. API端点(LANGSMITH_ENDPOINT)必须指向正确的LangSmith API地址
 * 4. 追踪开关(LANGSMITH_TRACING_ENABLED)必须设置为true
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Slf4j
@Service
public class LangSmithService {

    @Autowired
    private ApiConfig apiConfig;

    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;

    // In-memory storage for traces (in production, use database)
    private final Map<String, RunTrace> runs = new ConcurrentHashMap<>();
    private final Map<String, List<TraceEvent>> traces = new ConcurrentHashMap<>();

    public LangSmithService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * LANGSMITH: End-to-end Tracing - Start a new run
     *
     * @param runId Unique run identifier
     * @param threadId Thread identifier
     * @param runType Type of run (CODE_DEVELOPMENT, etc.)
     * @param inputs Initial inputs
     */
    public void startRun(String runId, String threadId, String runType, Map<String, Object> inputs) {
        log.info("LANGSMITH: Starting run trace: {} for thread: {}", runId, threadId);

        RunTrace run = RunTrace.builder()
                .id(runId)
                .threadId(threadId)
                .runType(runType)
                .status("IN_PROGRESS")
                .startedAt(System.currentTimeMillis())
                .inputs(inputs)
                .outputs(new HashMap<>())
                .traceEvents(new ArrayList<>())
                .build();

        runs.put(runId, run);
        traces.put(runId, new ArrayList<>());

        // Send to LangSmith if enabled
        if (apiConfig.getLangsmith().isValid() && apiConfig.getLangsmith().getTracingEnabled()) {
            sendRunToLangSmith(run);
        }
    }

    /**
     * LANGSMITH: Tracing - Record a trace event (node execution)
     *
     * @param runId Run identifier
     * @param nodeName Name of the node being executed
     * @param inputs Node inputs
     * @param outputs Node outputs
     * @param tokenUsage Token count for LLM calls
     * @param latency Execution time in milliseconds
     */
    public void recordTrace(String runId, String nodeName, Map<String, Object> inputs,
                           Map<String, Object> outputs, int tokenUsage, long latency) {
        log.debug("LANGSMITH: Recording trace for node: {}, tokens: {}, latency: {}ms",
                nodeName, tokenUsage, latency);

        TraceEvent trace = TraceEvent.builder()
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .nodeName(nodeName)
                .traceType("NODE_EXECUTION")
                .inputs(inputs)
                .outputs(outputs)
                .startedAt(System.currentTimeMillis() - latency)
                .endedAt(System.currentTimeMillis())
                .status("COMPLETED")
                .tokenUsage(tokenUsage)
                .latencyMs(latency)
                .build();

        traces.computeIfPresent(runId, (k, v) -> {
            v.add(trace);
            return v;
        });

        runs.computeIfPresent(runId, (k, v) -> {
            v.getTraceEvents().add(trace);
            return v;
        });
    }

    /**
     * LANGSMITH: Tracing - Record checkpoint event
     *
     * @param runId Run identifier
     * @param checkpointName Checkpoint name
     * @param currentNode Current node in workflow
     */
    public void recordCheckpoint(String runId, String checkpointName, String currentNode) {
        log.debug("LANGSMITH: Recording checkpoint: {} at node: {}", checkpointName, currentNode);

        Map<String, Object> checkpointInputs = new HashMap<>();
        checkpointInputs.put("checkpoint_name", checkpointName);
        checkpointInputs.put("current_node", currentNode);

        Map<String, Object> checkpointOutputs = new HashMap<>();
        checkpointOutputs.put("checkpoint_saved", true);

        TraceEvent trace = TraceEvent.builder()
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .nodeName("CheckpointManager")
                .traceType("CHECKPOINT")
                .inputs(checkpointInputs)
                .outputs(checkpointOutputs)
                .startedAt(System.currentTimeMillis())
                .endedAt(System.currentTimeMillis())
                .status("COMPLETED")
                .build();

        traces.computeIfPresent(runId, (k, v) -> {
            v.add(trace);
            return v;
        });
    }

    /**
     * LANGSMITH: Performance Metrics - Record run metrics
     *
     * @param runId Run identifier
     * @param totalTokens Total tokens consumed
     * @param duration Total execution time in milliseconds
     * @param iterationCount Number of workflow iterations
     * @param revisionCount Number of revisions (Reviewer → Coder cycles)
     */
    public void recordRunMetrics(String runId, int totalTokens, long duration,
                                int iterationCount, int revisionCount) {
        log.info("LANGSMITH: Recording metrics - Tokens: {}, Duration: {}ms, Iterations: {}, Revisions: {}",
                totalTokens, duration, iterationCount, revisionCount);

        runs.computeIfPresent(runId, (k, v) -> {
            v.setTotalTokens(totalTokens);
            v.setTotalDuration(duration);
            v.setIterationCount(iterationCount);
            v.setRevisionCount(revisionCount);
            return v;
        });
    }

    /**
     * LANGSMITH: End-to-end Tracing - End a run
     *
     * @param runId Run identifier
     * @param status Final status (COMPLETED, FAILED)
     * @param errorMessage Error message if failed
     */
    public void endRun(String runId, String status, String errorMessage) {
        log.info("LANGSMITH: Ending run: {} with status: {}", runId, status);

        runs.computeIfPresent(runId, (k, v) -> {
            v.setStatus(status);
            v.setEndedAt(System.currentTimeMillis());
            v.setErrorMessage(errorMessage);
            return v;
        });

        // Send final results to LangSmith
        if (apiConfig.getLangsmith().isValid() && apiConfig.getLangsmith().getTracingEnabled()) {
            RunTrace run = runs.get(runId);
            if (run != null) {
                sendRunUpdateToLangSmith(run);
            }
        }
    }

    /**
     * LANGSMITH: Debugging - Get run details
     *
     * @param runId Run identifier
     * @return Complete run trace
     */
    public RunTrace getRunTrace(String runId) {
        log.debug("LANGSMITH: Retrieving trace for run: {}", runId);
        return runs.get(runId);
    }

    /**
     * LANGSMITH: Debugging - List all runs
     *
     * @return List of all runs
     */
    public List<RunTrace> listRuns() {
        return new ArrayList<>(runs.values());
    }

    /**
     * LANGSMITH: Debugging - Get traces for a run
     *
     * @param runId Run identifier
     * @return List of trace events
     */
    public List<TraceEvent> getTraces(String runId) {
        return traces.getOrDefault(runId, new ArrayList<>());
    }

    /**
     * LANGSMITH: Evaluation Framework - Evaluate run results
     *
     * @param runId Run identifier
     * @param evaluationType Type of evaluation
     * @return Evaluation result
     */
    public EvaluationResult evaluateRun(String runId, String evaluationType) {
        log.info("LANGSMITH: Evaluating run: {} with type: {}", runId, evaluationType);

        RunTrace run = runs.get(runId);
        if (run == null) {
            return EvaluationResult.builder()
                    .runId(runId)
                    .evaluationType(evaluationType)
                    .score(0.0)
                    .passed(false)
                    .feedback("Run not found")
                    .build();
        }

        // Simple evaluation logic based on run metrics
        double score = calculateScore(run, evaluationType);
        boolean passed = score >= 70.0;
        String feedback = generateFeedback(run, evaluationType, score);

        return EvaluationResult.builder()
                .id(UUID.randomUUID().toString())
                .runId(runId)
                .evaluationType(evaluationType)
                .score(score)
                .passed(passed)
                .feedback(feedback)
                .evaluatedAt(System.currentTimeMillis())
                .build();
    }

    /**
     * LANGSMITH: Performance Metrics - Get performance metrics
     *
     * @param runId Run identifier
     * @return Performance metrics
     */
    public PerformanceMetrics getPerformanceMetrics(String runId) {
        RunTrace run = runs.get(runId);
        if (run == null) {
            return null;
        }

        return PerformanceMetrics.builder()
                .runId(runId)
                .totalTokens(run.getTotalTokens())
                .promptTokens(run.getTotalTokens() / 2) // Approximate
                .completionTokens(run.getTotalTokens() / 2) // Approximate
                .totalLatencyMs(run.getTotalDuration())
                .totalCost(calculateCost(run.getTotalTokens()))
                .iterationCount(run.getIterationCount())
                .revisionCount(run.getRevisionCount())
                .build();
    }

    // ========== Helper Methods ==========

    /**
     * LANGSMITH: 发送运行数据到LangSmith云平台
     * Send run data to LangSmith cloud platform for visualization
     *
     * LangSmith API集成说明:
     * - 使用LangSmith Runs API创建和更新运行记录
     * - 支持实时追踪和调试功能
     * - 需要有效的API密钥和项目配置
     *
     * @param run 追踪的运行数据
     */
    private void sendRunToLangSmith(RunTrace run) {
        try {
            // LANGSMITH: 验证配置完整性
            if (!apiConfig.getLangsmith().isValid()) {
                log.warn("LANGSMITH: 配置不完整 - 跳过发送数据到LangSmith。请检查API密钥和项目配置。");
                log.warn("LANGSMITH: Configuration incomplete - Check LANGSMITH_API_KEY and LANGSMITH_PROJECT in application.yml");
                return;
            }

            // LANGSMITH: 构建符合LangSmith API格式的请求体
            Map<String, Object> smithRun = new HashMap<>();
            smithRun.put("id", run.getId());
            smithRun.put("name", "Code Development Workflow");
            smithRun.put("project_name", apiConfig.getLangsmith().getProject());
            smithRun.put("start_time", new Date(run.getStartedAt()));
            smithRun.put("inputs", run.getInputs());
            smithRun.put("outputs", run.getOutputs());
            smithRun.put("run_type", "llm");
            smithRun.put("status", run.getStatus().toLowerCase());
            smithRun.put("thread_id", run.getThreadId());

            // 添加额外的元数据
            Map<String, Object> extra = new HashMap<>();
            extra.put("run_type", run.getRunType());
            extra.put("iteration_count", run.getIterationCount());
            extra.put("revision_count", run.getRevisionCount());
            extra.put("total_tokens", run.getTotalTokens());
            smithRun.put("extra", extra);

            String jsonBody = objectMapper.writeValueAsString(smithRun);

            // LANGSMITH: 使用正确的LangSmith API端点
            String apiUrl = apiConfig.getLangsmith().getUrl() + "/runs";

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiConfig.getLangsmith().getKey())
                    .addHeader("X-API-Key", apiConfig.getLangsmith().getKey()) // LangSmith备用认证头
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            log.info("LANGSMITH: 发送运行数据到LangSmith - Run ID: {}, Project: {}",
                    run.getId(), apiConfig.getLangsmith().getProject());

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                log.info("LANGSMITH: 成功发送运行数据到LangSmith - Status: {}", response.code());
            } else {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                log.error("LANGSMITH: 发送运行数据失败 - HTTP {}: {}", response.code(), errorBody);
                log.error("LANGSMITH: Failed to send run to LangSmith - Check API key and project name");
            }
        } catch (Exception e) {
            log.error("LANGSMITH: 发送运行数据时发生错误: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            log.error("LANGSMITH: Error sending run to LangSmith - Check network connectivity and API configuration");
        }
    }

    /**
     * LANGSMITH: 更新LangSmith中的运行数据
     * Update existing run data in LangSmith cloud platform
     *
     * LangSmith API更新说明:
     * - 使用PATCH方法更新现有运行记录
     * - 更新最终状态、输出和性能指标
     * - 确保LangSmith平台显示完整的工作流结果
     *
     * @param run 要更新的运行数据
     */
    private void sendRunUpdateToLangSmith(RunTrace run) {
        try {
            // LANGSMITH: 验证配置完整性
            if (!apiConfig.getLangsmith().isValid()) {
                log.warn("LANGSMITH: 配置不完整 - 跳过更新LangSmith数据");
                return;
            }

            // LANGSMITH: 构建更新数据
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("status", run.getStatus().toLowerCase());
            updateData.put("outputs", run.getOutputs());
            updateData.put("end_time", new Date(run.getEndedAt()));

            // 添加性能指标
            if (run.getTotalTokens() != null) {
                Map<String, Object> extra = new HashMap<>();
                extra.put("total_tokens", run.getTotalTokens());
                extra.put("total_duration_ms", run.getTotalDuration());
                extra.put("iteration_count", run.getIterationCount());
                extra.put("revision_count", run.getRevisionCount());
                updateData.put("extra", extra);
            }

            String jsonBody = objectMapper.writeValueAsString(updateData);

            // LANGSMITH: 使用正确的LangSmith API端点进行更新
            String apiUrl = apiConfig.getLangsmith().getUrl() + "/runs/" + run.getId();

            Request request = new Request.Builder()
                    .url(apiUrl)
                    .addHeader("Authorization", "Bearer " + apiConfig.getLangsmith().getKey())
                    .addHeader("X-API-Key", apiConfig.getLangsmith().getKey()) // LangSmith备用认证头
                    .addHeader("Content-Type", "application/json")
                    .patch(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                    .build();

            log.info("LANGSMITH: 更新LangSmith运行数据 - Run ID: {}, Status: {}",
                    run.getId(), run.getStatus());

            Response response = httpClient.newCall(request).execute();

            if (response.isSuccessful()) {
                log.info("LANGSMITH: 成功更新运行数据 - Status: {}", response.code());
            } else {
                String errorBody = response.body() != null ? response.body().string() : "无响应体";
                log.error("LANGSMITH: 更新运行数据失败 - HTTP {}: {}", response.code(), errorBody);
            }
        } catch (Exception e) {
            log.error("LANGSMITH: 更新运行数据时发生错误: {} - {}", e.getClass().getSimpleName(), e.getMessage());
        }
    }

    private double calculateScore(RunTrace run, String evaluationType) {
        switch (evaluationType) {
            case "EFFICIENCY":
                // Score based on iteration count (fewer is better)
                return Math.max(0, 100 - (run.getIterationCount() * 10));
            case "QUALITY":
                // Score based on revision count (fewer revisions = better quality)
                return Math.max(0, 100 - (run.getRevisionCount() * 15));
            case "PERFORMANCE":
                // Score based on duration (faster is better)
                long durationSeconds = run.getTotalDuration() / 1000;
                return Math.max(0, 100 - (durationSeconds / 10));
            default:
                return 75.0; // Default score
        }
    }

    private String generateFeedback(RunTrace run, String evaluationType, double score) {
        switch (evaluationType) {
            case "EFFICIENCY":
                return String.format("Workflow completed %d iterations. %s",
                        run.getIterationCount(),
                        score >= 70 ? "Good efficiency." : "Consider optimizing workflow.");
            case "QUALITY":
                return String.format("Code required %d revisions. %s",
                        run.getRevisionCount(),
                        score >= 70 ? "Good quality on first attempt." : "Multiple revisions needed.");
            case "PERFORMANCE":
                return String.format("Total execution time: %d seconds. %s",
                        run.getTotalDuration() / 1000,
                        score >= 70 ? "Acceptable performance." : "Performance could be improved.");
            default:
                return "Evaluation complete.";
        }
    }

    private double calculateCost(int totalTokens) {
        // Simple cost calculation (adjust based on actual DeepSeek pricing)
        return totalTokens * 0.0001; // $0.10 per 1M tokens
    }

    // ========== Inner Classes ==========

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RunTrace {
        private String id;
        private String threadId;
        private String runType;
        private String status;
        private Long startedAt;
        private Long endedAt;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
        private String errorMessage;
        private List<TraceEvent> traceEvents;
        private Integer totalTokens;
        private Long totalDuration;
        private Integer iterationCount;
        private Integer revisionCount;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TraceEvent {
        private String id;
        private String runId;
        private String nodeName;
        private String traceType;
        private Map<String, Object> inputs;
        private Map<String, Object> outputs;
        private Long startedAt;
        private Long endedAt;
        private String status;
        private String errorMessage;
        private Integer tokenUsage;
        private Long latencyMs;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class EvaluationResult {
        private String id;
        private String runId;
        private String evaluationType;
        private Double score;
        private Boolean passed;
        private String feedback;
        private Long evaluatedAt;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PerformanceMetrics {
        private String runId;
        private Integer totalTokens;
        private Integer promptTokens;
        private Integer completionTokens;
        private Long totalLatencyMs;
        private Double totalCost;
        private Integer iterationCount;
        private Integer revisionCount;
    }
}
