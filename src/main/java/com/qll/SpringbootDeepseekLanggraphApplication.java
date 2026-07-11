package com.qll;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

/**
 * SpringBoot DeepSeek LangGraph Demo Application
 *
 * This application demonstrates:
 * - LANGGRAPH: Stateful, multi-agent workflows
 * - LANGGRAPH: Cyclic workflows with loops
 * - LANGGRAPH: Multi-agent coordination
 * - LANGGRAPH: Checkpointing and state persistence
 * - LANGSMITH: End-to-end tracing
 * - LANGSMITH: Debugging and observability
 * - LANGSMITH: Evaluation framework
 * - LANGSMITH: Performance metrics
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@SpringBootApplication
@ComponentScan(basePackages = "com.qll")
public class SpringbootDeepseekLanggraphApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootDeepseekLanggraphApplication.class, args);
    }
}
