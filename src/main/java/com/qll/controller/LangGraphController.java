package com.qll.controller;

import com.qll.langgraph.workflows.MultiAgentWorkflow;
import com.qll.model.GraphState;
import com.qll.service.LangSmithService;
import com.qll.service.LangSmithService.EvaluationResult;
import com.qll.service.LangSmithService.PerformanceMetrics;
import com.qll.service.LangSmithService.RunTrace;
import com.qll.service.LangSmithService.TraceEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LangGraph & LangSmith演示REST API控制器
 * REST API Controller for LangGraph & LangSmith Demo
 *
 * 提供的端点 (Exposes endpoints for):
 * - LANGGRAPH: 多智能体工作流执行 (Multi-agent workflow execution)
 * - LANGGRAPH: 状态持久化和检查点 (State persistence and checkpointing)
 * - LANGSMITH: 追踪和可观测性 (Tracing and observability)
 * - LANGSMITH: 评估和指标 (Evaluation and metrics)
 *
 * API端点说明 (API Endpoint Notes):
 * 所有端点基础路径为 /api/workflow
 * 支持跨域请求访问前端应用
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Slf4j
@RestController
@RequestMapping("/workflow")
@CrossOrigin(origins = "*")
public class LangGraphController {

    @Autowired
    private MultiAgentWorkflow multiAgentWorkflow;

    @Autowired(required = false)
    private LangSmithService langSmithService;

    /**
     * LANGGRAPH: 启动开发周期 (Start Development Cycle)
     * POST /api/workflow/start
     *
     * 启动研究 → 编码 → 审查工作流
     * Initiates the Researcher → Coder → Reviewer workflow
     *
     * LANGGRAPH功能演示 (LANGGRAPH Features demonstrated):
     * - 多智能体协调 (Multi-agent Coordination): 协调三个智能体工作
     * - 状态持久化 (State Persistence): 跨工作流维护状态
     * - 循环工作流 (Cyclic Workflows): 支持审查 → 编码循环
     *
     * @param request 包含需求描述和可选线程ID的请求
     *                Request containing requirements and optional thread ID
     * @return 工作流执行结果 (Workflow execution result)
     */
    @PostMapping("/start")
    public ResponseEntity<GraphState> startDevelopmentCycle(@RequestBody WorkflowRequest request) {
        log.info("LANGGRAPH: Starting development cycle for requirements: {}",
                request.getRequirements().substring(0, Math.min(50, request.getRequirements().length())));

        try {
            // LANGGRAPH: 使用状态持久化执行工作流
            // Execute workflow with state persistence
            GraphState result = multiAgentWorkflow.executeWorkflow(
                    request.getRequirements(),
                    request.getThreadId()
            );

            log.info("LANGGRAPH: Development cycle completed. Status: {}, Revisions: {}",
                    result.getStatus(), result.getRevisionCount());

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error starting development cycle: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGGRAPH: View Development State
     * GET /api/workflow/state/{threadId}
     *
     * Retrieves current workflow state
     *
     * LANGGRAPH Features demonstrated:
     * - State Persistence: Retrieves saved state
     *
     * @param threadId Thread identifier
     * @return Current workflow state
     */
    @GetMapping("/state/{threadId}")
    public ResponseEntity<GraphState> getDevelopmentState(@PathVariable String threadId) {
        log.info("LANGGRAPH: Retrieving state for thread: {}", threadId);

        try {
            GraphState state = multiAgentWorkflow.getWorkflowState(threadId);
            return ResponseEntity.ok(state);

        } catch (Exception e) {
            log.error("Error retrieving state: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGGRAPH: Pause & Resume Development
     * POST /api/workflow/resume
     *
     * Resumes workflow from a checkpoint
     *
     * LANGGRAPH Features demonstrated:
     * - Checkpointing: Resumes from saved state
     * - State Persistence: Maintains state across pause/resume
     *
     * @param request Request containing thread ID and checkpoint name
     * @return Resumed workflow state
     */
    @PostMapping("/resume")
    public ResponseEntity<GraphState> resumeDevelopment(@RequestBody ResumeRequest request) {
        log.info("LANGGRAPH: Resuming development from checkpoint: {} for thread: {}",
                request.getCheckpointName(), request.getThreadId());

        try {
            // LANGGRAPH: Resume from checkpoint
            GraphState result = multiAgentWorkflow.resumeFromCheckpoint(
                    request.getThreadId(),
                    request.getCheckpointName(),
                    request.getUpdatedRequirements()
            );

            log.info("LANGGRAPH: Development resumed. Status: {}", result.getStatus());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error resuming development: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGGRAPH: Request Revision
     * POST /api/workflow/revision
     *
     * Manually triggers a revision cycle (Reviewer → Coder loop)
     *
     * LANGGRAPH Features demonstrated:
     * - Cyclic Workflows: Triggers Reviewer → Coder cycle
     * - Multi-agent Coordination: Coordinates revision between agents
     *
     * @param request Request containing thread ID and revision comments
     * @return Updated workflow state
     */
    @PostMapping("/revision")
    public ResponseEntity<GraphState> requestRevision(@RequestBody RevisionRequest request) {
        log.info("LANGGRAPH: Requesting revision for thread: {}", request.getThreadId());

        try {
            // Get current state
            GraphState state = multiAgentWorkflow.getWorkflowState(request.getThreadId());

            if (state == null) {
                return ResponseEntity.notFound().build();
            }

            // Update with revision comments
            state.setReviewComments(request.getRevisionComments());
            state.setReviewStatus("NEEDS_REVISION");

            // Resume workflow to trigger Coder node
            GraphState result = multiAgentWorkflow.resumeFromCheckpoint(
                    request.getThreadId(),
                    "Coder_BEFORE",
                    null
            );

            log.info("LANGGRAPH: Revision requested. Revision count: {}", result.getRevisionCount());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error requesting revision: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGGRAPH: Reset Development
     * DELETE /api/workflow/reset/{threadId}
     *
     * Clears workflow state and starts fresh
     *
     * LANGGRAPH Features demonstrated:
     * - State Persistence: Clears saved state
     *
     * @param threadId Thread identifier
     * @return Success response
     */
    @DeleteMapping("/reset/{threadId}")
    public ResponseEntity<Map<String, Object>> resetDevelopment(@PathVariable String threadId) {
        log.info("LANGGRAPH: Resetting development for thread: {}", threadId);

        try {
            // LANGGRAPH: Reset workflow state
            multiAgentWorkflow.resetWorkflow(threadId);

            Map<String, Object> responseMap = new HashMap<>();
            responseMap.put("message", "Workflow reset successfully");
            responseMap.put("threadId", threadId);
            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            log.error("Error resetting development: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGGRAPH: View Workflow Graph
     * GET /api/workflow/graph
     *
     * Returns the workflow graph structure
     *
     * @return Workflow graph representation
     */
    @GetMapping("/graph")
    public ResponseEntity<WorkflowGraph> getWorkflowGraph() {
        log.info("LANGGRAPH: Retrieving workflow graph structure");

        List<WorkflowNode> nodes = new ArrayList<>();
        nodes.add(WorkflowNode.builder()
                .id("researcher")
                .name("Researcher")
                .type("RESEARCHER")
                .description("Gathers technical requirements and researches APIs")
                .build());
        nodes.add(WorkflowNode.builder()
                .id("coder")
                .name("Coder")
                .type("CODER")
                .description("Writes code based on research findings")
                .build());
        nodes.add(WorkflowNode.builder()
                .id("reviewer")
                .name("Reviewer")
                .type("REVIEWER")
                .description("Reviews code and requests revisions")
                .build());

        List<WorkflowEdge> edges = new ArrayList<>();
        edges.add(WorkflowEdge.builder()
                .from("START")
                .to("researcher")
                .label("Start")
                .build());
        edges.add(WorkflowEdge.builder()
                .from("researcher")
                .to("coder")
                .label("Research complete")
                .build());
        edges.add(WorkflowEdge.builder()
                .from("coder")
                .to("reviewer")
                .label("Code ready")
                .build());
        edges.add(WorkflowEdge.builder()
                .from("reviewer")
                .to("coder")
                .label("Needs revision")
                .conditional(true)
                .build());
        edges.add(WorkflowEdge.builder()
                .from("reviewer")
                .to("END")
                .label("Approved")
                .conditional(true)
                .build());

        WorkflowGraph graph = WorkflowGraph.builder()
                .nodes(nodes)
                .edges(edges)
                .build();

        return ResponseEntity.ok(graph);
    }

    // ========== LANGSMITH Endpoints ==========

    /**
     * LANGSMITH: View Development Traces
     * GET /api/workflow/traces
     *
     * Lists all recent development cycles (runs)
     *
     * LANGSMITH Features demonstrated:
     * - End-to-end Tracing: Returns all traced runs
     *
     * @return List of run traces
     */
    @GetMapping("/traces")
    public ResponseEntity<List<?>> getTraces() {
        log.info("LANGSMITH: Retrieving all traces");

        if (langSmithService == null) {
            return ResponseEntity.ok(new ArrayList<>());
        }

        try {
            List<?> traces = langSmithService.listRuns();
            return ResponseEntity.ok(traces);

        } catch (Exception e) {
            log.error("Error retrieving traces: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGSMITH: Trace Detail View
     * GET /api/workflow/traces/{runId}
     *
     * Shows detailed trace with all events and timings
     *
     * LANGSMITH Features demonstrated:
     * - Debugging: Detailed view of execution
     * - Tracing: Complete execution trace
     *
     * @param runId Run identifier
     * @return Detailed trace information
     */
    @GetMapping("/traces/{runId}")
    public ResponseEntity<?> getTraceDetail(@PathVariable String runId) {
        log.info("LANGSMITH: Retrieving trace detail for run: {}", runId);

        if (langSmithService == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            RunTrace runTrace = langSmithService.getRunTrace(runId);
            List<TraceEvent> traceEvents = langSmithService.getTraces(runId);

            Map<String, Object> detail = new HashMap<>();
            detail.put("run", runTrace);
            detail.put("events", traceEvents);

            return ResponseEntity.ok(detail);

        } catch (Exception e) {
            log.error("Error retrieving trace detail: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGSMITH: Run Development Evaluation
     * POST /api/workflow/evaluate
     *
     * Evaluates a development run against different criteria
     *
     * LANGSMITH Features demonstrated:
     * - Evaluation Framework: Evaluates workflow execution
     * - Performance Metrics: Assesses efficiency and quality
     *
     * @param request Request containing run ID and evaluation type
     * @return Evaluation result
     */
    @PostMapping("/evaluate")
    public ResponseEntity<?> evaluateRun(@RequestBody EvaluationRequest request) {
        log.info("LANGSMITH: Evaluating run: {} with type: {}",
                request.getRunId(), request.getEvaluationType());

        if (langSmithService == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            EvaluationResult result = langSmithService.evaluateRun(request.getRunId(), request.getEvaluationType());
            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Error evaluating run: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGSMITH: Performance Metrics
     * GET /api/workflow/metrics/{runId}
     *
     * Returns performance metrics for a run
     *
     * LANGSMITH Features demonstrated:
     * - Performance Metrics: Token usage, latency, costs
     *
     * @param runId Run identifier
     * @return Performance metrics
     */
    @GetMapping("/metrics/{runId}")
    public ResponseEntity<?> getPerformanceMetrics(@PathVariable String runId) {
        log.info("LANGSMITH: Retrieving performance metrics for run: {}", runId);

        if (langSmithService == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            PerformanceMetrics metrics = langSmithService.getPerformanceMetrics(runId);
            return ResponseEntity.ok(metrics);

        } catch (Exception e) {
            log.error("Error retrieving metrics: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * LANGSMITH: Compare Development Cycles
     * GET /api/workflow/compare
     *
     * Compares multiple runs side-by-side
     *
     * LANGSMITH Features demonstrated:
     * - Evaluation Framework: Comparative analysis
     *
     * @param runIds Comma-separated list of run IDs
     * @return Comparison results
     */
    @GetMapping("/compare")
    public ResponseEntity<?> compareRuns(@RequestParam String runIds) {
        log.info("LANGSMITH: Comparing runs: {}", runIds);

        if (langSmithService == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String[] ids = runIds.split(",");
            List<Map<String, Object>> comparisons = new java.util.ArrayList<>();

            for (String runId : ids) {
                RunTrace runTrace = langSmithService.getRunTrace(runId.trim());
                PerformanceMetrics metrics = langSmithService.getPerformanceMetrics(runId.trim());

                if (runTrace != null) {
                    Map<String, Object> comparison = new HashMap<>();
                    comparison.put("runId", runId.trim());
                    comparison.put("trace", runTrace);
                    comparison.put("metrics", metrics);
                    comparisons.add(comparison);
                }
            }

            return ResponseEntity.ok(comparisons);

        } catch (Exception e) {
            log.error("Error comparing runs: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // ========== Request/Response DTOs ==========

    @lombok.Data
    public static class WorkflowRequest {
        private String requirements;
        private String threadId;
    }

    @lombok.Data
    public static class ResumeRequest {
        private String threadId;
        private String checkpointName;
        private String updatedRequirements;
    }

    @lombok.Data
    public static class RevisionRequest {
        private String threadId;
        private String revisionComments;
    }

    @lombok.Data
    public static class EvaluationRequest {
        private String runId;
        private String evaluationType; // EFFICIENCY, QUALITY, PERFORMANCE
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkflowGraph {
        private List<WorkflowNode> nodes;
        private List<WorkflowEdge> edges;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkflowNode {
        private String id;
        private String name;
        private String type;
        private String description;
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class WorkflowEdge {
        private String from;
        private String to;
        private String label;
        private boolean conditional;
    }
}
