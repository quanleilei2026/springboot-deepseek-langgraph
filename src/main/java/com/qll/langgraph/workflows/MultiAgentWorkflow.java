package com.qll.langgraph.workflows;

import com.qll.config.LangGraphConfig;
import com.qll.langgraph.nodes.CoderNode;
import com.qll.langgraph.nodes.ResearcherNode;
import com.qll.langgraph.nodes.ReviewerNode;
import com.qll.model.GraphState;
import com.qll.service.LangSmithService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LANGGRAPH: Multi-Agent Workflow
 * Orchestrates the Researcher → Coder → Reviewer workflow
 *
 * Core LANGGRAPH Features demonstrated:
 * - State Persistence: State maintained across all nodes
 * - Cyclic Workflows: Reviewer → Coder loops for revisions
 * - Multi-agent Coordination: Three agents working together
 * - Checkpointing: State saved at each node for resumption
 *
 * Core LANGSMITH Features integrated:
 * - End-to-end Tracing: Every step is traced
 * - Performance Metrics: Tokens, latency tracked
 * - Evaluation: Workflow evaluated at completion
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Slf4j
@Component
public class MultiAgentWorkflow {

    @Autowired
    private ResearcherNode researcherNode;

    @Autowired
    private CoderNode coderNode;

    @Autowired
    private ReviewerNode reviewerNode;

    @Autowired
    private LangGraphConfig langGraphConfig;

    @Autowired(required = false)
    private LangSmithService langSmithService;

    // In-memory state storage for LANGGRAPH: State Persistence
    private final Map<String, GraphState> stateStore = new ConcurrentHashMap<>();

    /**
     * Execute the multi-agent workflow
     * LANGGRAPH: Demonstrates complete workflow with state persistence
     *
     * @param requirements User requirements for code development
     * @param threadId Thread ID for state persistence (optional, creates new if null)
     * @return Final workflow state
     */
    public GraphState executeWorkflow(String requirements, String threadId) {
        // Create new thread if not provided
        if (threadId == null) {
            threadId = UUID.randomUUID().toString();
        }

        String runId = UUID.randomUUID().toString();

        log.info("LANGGRAPH: Starting workflow for thread: {}, run: {}", threadId, runId);

        // LANGGRAPH: State Persistence - Initialize state
        GraphState state = GraphState.initialState(threadId, runId, requirements);

        // LANGSMITH: Tracing - Start run trace
        if (langSmithService != null) {
            langSmithService.startRun(runId, threadId, "CODE_DEVELOPMENT",
                    buildInitialInputs(requirements));
        }

        long startTime = System.currentTimeMillis();
        int totalTokens = 0;

        try {
            // LANGGRAPH: Multi-agent Coordination
            // Execute workflow with checkpointing
            state = executeWorkflowWithCheckpointing(state);

            // Calculate workflow metrics
            long duration = System.currentTimeMillis() - startTime;

            // LANGSMITH: Performance Metrics - Record metrics
            if (langSmithService != null) {
                langSmithService.recordRunMetrics(runId, totalTokens, duration,
                        state.getIterationCount(), state.getRevisionCount());
                langSmithService.endRun(runId, state.getStatus(), state.getErrorMessage());
            }

            // LANGGRAPH: State Persistence - Save state
            stateStore.put(threadId, state);

            log.info("LANGGRAPH: Workflow completed. Status: {}, Iterations: {}, Revisions: {}",
                    state.getStatus(), state.getIterationCount(), state.getRevisionCount());

        } catch (Exception e) {
            log.error("LANGGRAPH: Workflow failed: {}", e.getMessage(), e);
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("errorMessage", "Workflow failed: " + e.getMessage());
            state.updateState("ERROR", errorMap);

            if (langSmithService != null) {
                langSmithService.endRun(runId, "FAILED", e.getMessage());
            }
        }

        return state;
    }

    /**
     * Execute workflow with checkpointing
     * LANGGRAPH: Checkpointing - State saved at each node
     *
     * @param state Initial state
     * @return Final state
     */
    private GraphState executeWorkflowWithCheckpointing(GraphState state) {
        String currentNode = "START";
        int iteration = 0;
        final int maxIterations = langGraphConfig.getWorkflow().getMaxIterations();

        while (!state.isComplete() && iteration < maxIterations) {
            iteration++;
            state.incrementIteration();

            log.info("LANGGRAPH: Iteration {}, Current node: {}, Thread: {}",
                    iteration, currentNode, state.getThreadId());

            // LANGGRAPH: Checkpointing - Save state before executing node
            saveCheckpoint(state, currentNode + "_BEFORE");

            // Execute current node
            switch (currentNode) {
                case "START":
                    currentNode = "Researcher";
                    state = researcherNode.execute(state, state.getRequirements());
                    currentNode = researcherNode.determineNextNode(state);
                    break;

                case "Researcher":
                    state = researcherNode.execute(state, state.getRequirements());
                    currentNode = researcherNode.determineNextNode(state);
                    break;

                case "Coder":
                    state = coderNode.execute(state, "");
                    currentNode = coderNode.determineNextNode(state);
                    break;

                case "Reviewer":
                    state = reviewerNode.execute(state, "");
                    currentNode = reviewerNode.determineNextNode(state);
                    break;

                case "END":
                    Map<String, Object> complete = new HashMap<>();
                    complete.put("status", "COMPLETED");
                    state.updateState("END", complete);
                    break;

                default:
                    log.warn("LANGGRAPH: Unknown node: {}", currentNode);
                    Map<String, Object> unknownNodeError = new HashMap<>();
                    unknownNodeError.put("errorMessage", "Unknown node: " + currentNode);
                    state.updateState("ERROR", unknownNodeError);
                    break;
            }

            // LANGGRAPH: Checkpointing - Save state after executing node
            saveCheckpoint(state, currentNode + "_AFTER");

            // LANGGRAPH: Check if workflow should continue
            if ("END".equals(currentNode) || state.isComplete()) {
                log.info("LANGGRAPH: Workflow reached end state");
                break;
            }

            if (state.getErrorMessage() != null) {
                log.error("LANGGRAPH: Workflow error encountered: {}", state.getErrorMessage());
                break;
            }
        }

        return state;
    }

    /**
     * Save checkpoint for workflow state
     * LANGGRAPH: Checkpointing - Enables workflow resumption
     *
     * @param state Current state
     * @param checkpointName Checkpoint identifier
     */
    private void saveCheckpoint(GraphState state, String checkpointName) {
        if (!langGraphConfig.getCheckpoint().getEnabled()) {
            return;
        }

        try {
            log.debug("LANGGRAPH: Saving checkpoint: {} for thread: {}",
                    checkpointName, state.getThreadId());

            // Store checkpoint information
            // In a full implementation, this would save to database
            // For demo purposes, we just log the checkpoint

            // LANGSMITH: Tracing - Record checkpoint event
            if (langSmithService != null) {
                langSmithService.recordCheckpoint(state.getRunId(), checkpointName,
                        state.getCurrentNode());
            }

        } catch (Exception e) {
            log.error("LANGGRAPH: Failed to save checkpoint: {}", e.getMessage());
        }
    }

    /**
     * Resume workflow from checkpoint
     * LANGGRAPH: Checkpointing - Resumes workflow from saved state
     *
     * @param threadId Thread ID
     * @param checkpointName Checkpoint to resume from
     * @param updatedRequirements Optional updated requirements
     * @return Resumed workflow state
     */
    public GraphState resumeFromCheckpoint(String threadId, String checkpointName,
                                             String updatedRequirements) {
        log.info("LANGGRAPH: Resuming workflow from checkpoint: {} for thread: {}",
                checkpointName, threadId);

        // In a full implementation, this would load state from database
        // For demo purposes, we execute a new workflow
        return executeWorkflow(updatedRequirements, threadId);
    }

    /**
     * Build initial inputs map for LANGSMITH tracing
     */
    private Map<String, Object> buildInitialInputs(String requirements) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("requirements", requirements);
        inputs.put("workflow_type", "CODE_DEVELOPMENT");
        return inputs;
    }

    /**
     * Get workflow status
     * LANGGRAPH: State Persistence - Returns current state
     *
     * @param threadId Thread ID
     * @return Current workflow state
     */
    public GraphState getWorkflowState(String threadId) {
        log.info("LANGGRAPH: Getting state for thread: {}", threadId);
        // LANGGRAPH: State Persistence - Retrieve saved state
        GraphState state = stateStore.get(threadId);
        if (state != null) {
            return state;
        }
        return GraphState.builder()
                .threadId(threadId)
                .status("NOT_FOUND")
                .build();
    }

    /**
     * Reset workflow
     * LANGGRAPH: State Persistence - Clears state for new workflow
     *
     * @param threadId Thread ID
     */
    public void resetWorkflow(String threadId) {
        log.info("LANGGRAPH: Resetting workflow for thread: {}", threadId);
        // LANGGRAPH: State Persistence - Clear stored state
        stateStore.remove(threadId);
    }
}
