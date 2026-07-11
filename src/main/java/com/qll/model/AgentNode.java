package com.qll.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AgentNode represents a node in the LANGGRAPH workflow
 *
 * LANGGRAPH Features demonstrated:
 * - Multi-agent Coordination: Each node represents an agent
 * - Stateful workflows: Nodes receive and update shared state
 * - Cyclic workflows: Nodes can transition back to previous nodes
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AgentNode {

    /**
     * Node name/identifier
     */
    private String name;

    /**
     * Node type (RESEARCHER, CODER, REVIEWER)
     */
    private String type;

    /**
     * Node description
     */
    private String description;

    /**
     * Agent prompt template
     */
    private String promptTemplate;

    /**
     * Next node(s) to transition to
     * Can be conditional for LANGGRAPH: Conditional Branching
     */
    private String[] nextNodes;

    /**
     * Whether this node can be revisited in cycles
     * LANGGRAPH: Cyclic Workflows
     */
    private boolean allowCycles;

    /**
     * Execute the node logic
     * This method processes the state and returns updated state
     *
     * @param state Current graph state
     * @param input Input for this node
     * @return Updated state
     */
    public GraphState execute(GraphState state, String input) {
        // This will be implemented by specific node types
        state.updateState(this.name, null);
        return state;
    }

    /**
     * Determine next node based on state
     * LANGGRAPH: Conditional Branching
     *
     * @param state Current graph state
     * @return Next node name
     */
    public String determineNextNode(GraphState state) {
        if (this.nextNodes == null || this.nextNodes.length == 0) {
            return "END";
        }

        // Simple logic: return first next node by default
        // Specific nodes can override this for conditional branching
        return this.nextNodes[0];
    }

    /**
     * Check if node should continue workflow
     * Can be overridden for conditional logic
     */
    public boolean shouldContinue(GraphState state) {
        return !state.isComplete();
    }
}
