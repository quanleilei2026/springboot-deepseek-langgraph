package com.qll.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LangGraph Configuration
 * Handles configuration for LangGraph workflow and state management
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "langgraph")
public class LangGraphConfig {

    /**
     * Thread management configuration for LANGGRAPH: State Persistence
     */
    private ThreadConfig thread = new ThreadConfig();

    /**
     * Checkpoint configuration for LANGGRAPH: Checkpointing
     */
    private CheckpointConfig checkpoint = new CheckpointConfig();

    /**
     * Workflow configuration
     */
    private WorkflowConfig workflow = new WorkflowConfig();

    /**
     * Thread Configuration for LANGGRAPH: State Persistence
     * Manages how thread states are stored and maintained
     */
    @Data
    public static class ThreadConfig {
        /**
         * Thread timeout in milliseconds
         */
        private Integer timeout = 300000; // 5 minutes default

        /**
         * Maximum number of states to keep per thread
         */
        private Integer maxStates = 100;
    }

    /**
     * Checkpoint Configuration for LANGGRAPH: Checkpointing
     * Manages how workflow states are saved for resumption
     */
    @Data
    public static class CheckpointConfig {
        /**
         * Enable/disable checkpointing
         */
        private Boolean enabled = true;

        /**
         * Interval between automatic checkpoints (ms)
         */
        private Integer saveInterval = 1000;
    }

    /**
     * Workflow Configuration
     */
    @Data
    public static class WorkflowConfig {
        /**
         * Maximum iterations before workflow termination
         */
        private Integer maxIterations = 10;

        /**
         * Number of retry attempts for failed nodes
         */
        private Integer retryAttempts = 3;
    }
}
