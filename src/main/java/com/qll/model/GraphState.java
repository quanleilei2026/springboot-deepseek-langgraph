package com.qll.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * LANGGRAPH: State Persistence
 * GraphState represents the state that persists across nodes in the workflow
 *
 * Core LANGGRAPH Features demonstrated:
 * - State Persistence: State is maintained across all nodes
 * - Multi-agent Coordination: All agents share and update this state
 * - Checkpointing: State can be saved at any point and resumed
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GraphState {

    /**
     * Thread ID for LANGGRAPH: State Persistence
     * Identifies the thread this state belongs to
     */
    private String threadId;

    /**
     * Run ID for LANGGRAPH workflow execution
     */
    private String runId;

    /**
     * Current node in the workflow
     */
    private String currentNode;

    /**
     * User requirements for code development
     */
    private String requirements;

    /**
     * Research findings from ResearcherNode
     */
    private String researchFindings;

    /**
     * Generated code from CoderNode
     */
    private String generatedCode;

    /**
     * Review comments from ReviewerNode
     */
    private String reviewComments;

    /**
     * Review status (APPROVED, NEEDS_REVISION)
     * This enables LANGGRAPH: Cyclic Workflows
     */
    private String reviewStatus;

    /**
     * Iteration count for tracking workflow cycles
     * Demonstrates LANGGRAPH: Cyclic Workflows
     */
    private int iterationCount;

    /**
     * Revision count for tracking Reviewer → Coder loops
     * Demonstrates LANGGRAPH: Conditional Branching
     */
    private int revisionCount;

    /**
     * Workflow status (IN_PROGRESS, COMPLETED, FAILED)
     */
    private String status;

    /**
     * Error messages if workflow fails
     */
    private String errorMessage;

    /**
     * Additional metadata for the state
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * List of completed nodes for tracking progress
     */
    @Builder.Default
    private List<String> completedNodes = new ArrayList<>();

    /**
     * Timestamp of last state update
     */
    private long lastUpdated;

    /**
     * Create initial state for new workflow
     */
    public static GraphState initialState(String threadId, String runId, String requirements) {
        return GraphState.builder()
                .threadId(threadId)
                .runId(runId)
                .requirements(requirements)
                .currentNode("START")
                .iterationCount(0)
                .revisionCount(0)
                .status("IN_PROGRESS")
                .lastUpdated(System.currentTimeMillis())
                .build();
    }

    /**
     * Update state with new values
     * LANGGRAPH: State Persistence - State is updated as workflow progresses
     */
    public void updateState(String nodeName, Map<String, Object> updates) {
        this.currentNode = nodeName;
        this.lastUpdated = System.currentTimeMillis();

        if (updates != null) {
            if (updates.containsKey("researchFindings")) {
                this.researchFindings = (String) updates.get("researchFindings");
            }
            if (updates.containsKey("generatedCode")) {
                this.generatedCode = (String) updates.get("generatedCode");
            }
            if (updates.containsKey("reviewComments")) {
                this.reviewComments = (String) updates.get("reviewComments");
            }
            if (updates.containsKey("reviewStatus")) {
                this.reviewStatus = (String) updates.get("reviewStatus");
            }
            if (updates.containsKey("status")) {
                this.status = (String) updates.get("status");
            }
            if (updates.containsKey("errorMessage")) {
                this.errorMessage = (String) updates.get("errorMessage");
            }
        }

        // Track completed nodes
        if (!this.completedNodes.contains(nodeName)) {
            this.completedNodes.add(nodeName);
        }
    }

    /**
     * Increment iteration count
     * LANGGRAPH: Cyclic Workflows
     */
    public void incrementIteration() {
        this.iterationCount++;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Increment revision count (when Reviewer → Coder loop occurs)
     * LANGGRAPH: Conditional Branching
     */
    public void incrementRevision() {
        this.revisionCount++;
        this.lastUpdated = System.currentTimeMillis();
    }

    /**
     * Check if workflow is complete
     */
    public boolean isComplete() {
        return "COMPLETED".equals(this.status);
    }

    /**
     * Check if revision is needed (Reviewer → Coder cycle)
     * LANGGRAPH: Conditional Branching
     */
    public boolean needsRevision() {
        return "NEEDS_REVISION".equals(this.reviewStatus);
    }
}
