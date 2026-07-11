package com.qll.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Centralized API Configuration Management (Requirement 9)
 * All API URLs and Keys are managed centrally for easy maintenance
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api")
public class ApiConfig {

    /**
     * DeepSeek API Configuration
     */
    private DeepSeekConfig deepseek = new DeepSeekConfig();

    /**
     * LangSmith API Configuration
     */
    private LangSmithConfig langsmith = new LangSmithConfig();

    /**
     * DeepSeek API Configuration
     */
    @Data
    public static class DeepSeekConfig {
        /**
         * API Base URL
         */
        private String url;

        /**
         * API Key for authentication
         */
        private String key;

        /**
         * Model name to use
         */
        private String model;

        /**
         * Request timeout in milliseconds
         */
        private Integer timeout = 60000;

        /**
         * Maximum tokens per response
         */
        private Integer maxTokens = 4000;

        /**
         * Temperature for response generation
         */
        private Double temperature = 0.7;

        /**
         * Validate configuration is complete
         */
        public boolean isValid() {
            return url != null && !url.isEmpty()
                    && key != null && !key.isEmpty()
                    && model != null && !model.isEmpty();
        }
    }

    /**
     * LangSmith API Configuration
     */
    @Data
    public static class LangSmithConfig {
        /**
         * API Base URL
         */
        private String url;

        /**
         * API Key for authentication
         */
        private String key;

        /**
         * Project name in LangSmith
         */
        private String project;

        /**
         * Endpoint for runs
         */
        private String endpoint;

        /**
         * Enable/disable tracing
         */
        private Boolean tracingEnabled = true;

        /**
         * Validate configuration is complete
         */
        public boolean isValid() {
            return url != null && !url.isEmpty()
                    && key != null && !key.isEmpty()
                    && project != null && !project.isEmpty();
        }
    }

    /**
     * Validate all API configurations
     */
    public boolean isValid() {
        return deepseek != null && deepseek.isValid()
                && langsmith != null && langsmith.isValid();
    }
}
