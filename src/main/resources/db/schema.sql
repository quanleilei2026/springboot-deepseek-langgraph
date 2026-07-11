-- MySQL 5.7.35 Database Schema for LangGraph & LangSmith Demo
-- Author: quanleilei
-- Date: 2026-02-28

-- Create database
CREATE DATABASE IF NOT EXISTS test
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE test;

-- ============================================================================
-- LANGGRAPH: State Persistence Tables
-- ============================================================================

/**
 * Thread table for LANGGRAPH: State Persistence
 * Stores thread information for managing conversation/workflow state
 */
CREATE TABLE IF NOT EXISTS threads (
    id VARCHAR(36) PRIMARY KEY,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    status VARCHAR(50) DEFAULT 'ACTIVE',
    metadata TEXT,
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Thread states table for LANGGRAPH: State Persistence
 * Stores all states for a thread, enabling checkpointing and resumption
 */
CREATE TABLE IF NOT EXISTS thread_states (
    id VARCHAR(36) PRIMARY KEY,
    thread_id VARCHAR(36) NOT NULL,
    state_data JSON NOT NULL,
    checkpoint_id VARCHAR(36),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    is_latest BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    INDEX idx_thread_id (thread_id),
    INDEX idx_checkpoint_id (checkpoint_id),
    INDEX idx_is_latest (is_latest)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Checkpoints table for LANGGRAPH: Checkpointing
 * Stores checkpoint information for workflow resumption
 */
CREATE TABLE IF NOT EXISTS checkpoints (
    id VARCHAR(36) PRIMARY KEY,
    thread_id VARCHAR(36) NOT NULL,
    thread_state_id VARCHAR(36) NOT NULL,
    checkpoint_name VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,
    FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    FOREIGN KEY (thread_state_id) REFERENCES thread_states(id) ON DELETE CASCADE,
    INDEX idx_thread_id (thread_id),
    INDEX idx_checkpoint_name (checkpoint_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- LANGSMITH: Tracing and Observability Tables
-- ============================================================================

/**
 * Runs table for LANGSMITH: End-to-end Tracing
 * Stores execution runs for the LangGraph workflow
 */
CREATE TABLE IF NOT EXISTS runs (
    id VARCHAR(36) PRIMARY KEY,
    thread_id VARCHAR(36),
    run_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    error_message TEXT,
    inputs JSON,
    outputs JSON,
    metadata TEXT,
    FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE SET NULL,
    INDEX idx_thread_id (thread_id),
    INDEX idx_status (status),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Traces table for LANGSMITH: End-to-end Tracing
 * Stores detailed trace information for each step in execution
 */
CREATE TABLE IF NOT EXISTS traces (
    id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    parent_trace_id VARCHAR(36),
    trace_type VARCHAR(50) NOT NULL,
    node_name VARCHAR(100),
    inputs JSON,
    outputs JSON,
    started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ended_at TIMESTAMP NULL,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    metadata TEXT,
    token_usage INT,
    latency_ms BIGINT,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE,
    FOREIGN KEY (parent_trace_id) REFERENCES traces(id) ON DELETE SET NULL,
    INDEX idx_run_id (run_id),
    INDEX idx_parent_trace_id (parent_trace_id),
    INDEX idx_trace_type (trace_type),
    INDEX idx_node_name (node_name),
    INDEX idx_started_at (started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Evaluations table for LANGSMITH: Evaluation Framework
 * Stores evaluation results for different prompts and configurations
 */
CREATE TABLE IF NOT EXISTS evaluations (
    id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    evaluation_type VARCHAR(50) NOT NULL,
    score DECIMAL(5,2),
    passed BOOLEAN,
    feedback TEXT,
    evaluated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    metadata TEXT,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE,
    INDEX idx_run_id (run_id),
    INDEX idx_evaluation_type (evaluation_type),
    INDEX idx_evaluated_at (evaluated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

/**
 * Performance metrics table for LANGSMITH: Performance Metrics
 * Stores performance and cost metrics for runs
 */
CREATE TABLE IF NOT EXISTS performance_metrics (
    id VARCHAR(36) PRIMARY KEY,
    run_id VARCHAR(36) NOT NULL,
    total_tokens INT NOT NULL,
    prompt_tokens INT NOT NULL,
    completion_tokens INT NOT NULL,
    total_latency_ms BIGINT NOT NULL,
    total_cost DECIMAL(10,6),
    iteration_count INT DEFAULT 0,
    revision_count INT DEFAULT 0,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE,
    INDEX idx_run_id (run_id),
    INDEX idx_recorded_at (recorded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- Application-specific tables for Code Development Workflow
-- ============================================================================

/**
 * Code development workflow states
 */
CREATE TABLE IF NOT EXISTS code_development_states (
    id VARCHAR(36) PRIMARY KEY,
    thread_id VARCHAR(36) NOT NULL,
    run_id VARCHAR(36) NOT NULL,
    requirements TEXT,
    research_findings TEXT,
    generated_code TEXT,
    review_comments TEXT,
    current_node VARCHAR(50),
    iteration_count INT DEFAULT 0,
    status VARCHAR(50) DEFAULT 'IN_PROGRESS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (thread_id) REFERENCES threads(id) ON DELETE CASCADE,
    FOREIGN KEY (run_id) REFERENCES runs(id) ON DELETE CASCADE,
    INDEX idx_thread_id (thread_id),
    INDEX idx_run_id (run_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Grant permissions (adjust username/password as needed)
-- GRANT ALL PRIVILEGES ON langgraph_demo.* TO 'root'@'localhost';
-- FLUSH PRIVILEGES;
