package com.qll.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * LangSmith配置类 (LangSmith Configuration Class)
 * 处理LangSmith追踪和可观测性的配置
 * Handles configuration for LangSmith tracing and observability
 *
 * LangSmith配置说明 (LangSmith Configuration Notes):
 * 1. 在application.yml中配置api.langsmith前缀的属性
 * 2. 配置有效的API密钥和项目名称以启用追踪功能
 * 3. 追踪数据可在LangSmith平台查看和调试
 *
 * @author quanleilei
 * @date 2026-02-28
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "api.langsmith")
public class LangSmithConfig {

    /**
     * LangSmith API基础URL (LangSmith API base URL)
     * 默认: https://api.smith.langchain.com
     * 用于连接LangSmith云平台
     */
    private String url;

    /**
     * API密钥 (API Key for authentication)
     * 获取地址: https://smith.langchain.com/
     * 必须配置有效密钥才能启用追踪功能
     */
    private String key;

    /**
     * LangSmith项目名称 (Project name in LangSmith)
     * 项目必须在LangSmith平台中存在
     * 追踪数据将显示在该项目下
     */
    private String project;

    /**
     * 运行和追踪的端点 (Endpoint for runs and traces)
     * 用于发送和接收追踪数据的API端点
     */
    private String endpoint;

    /**
     * 启用/禁用LANGSMITH追踪功能 (Enable/disable LANGSMITH tracing)
     * true: 启用追踪，false: 禁用追踪
     * 控制是否向LangSmith发送追踪数据
     */
    private Boolean tracingEnabled = true;

    /**
     * 批量发送追踪的大小 (Batch size for sending traces to LangSmith)
     * 控制批量发送追踪事件的数量
     */
    private Integer traceBatchSize = 10;

    /**
     * 内存中保持的最大追踪数量 (Maximum number of traces to keep in memory)
     * 防止内存溢出，控制内存使用
     */
    private Integer maxTracesInMemory = 1000;

    /**
     * 验证LANGSMITH集成配置是否完整
     * Validate configuration is complete for LANGSMITH integration
     *
     * @return 如果配置有效返回true，否则返回false
     *         Returns true if configuration is valid, false otherwise
     */
    public boolean isValid() {
        return url != null && !url.isEmpty()
                && key != null && !key.isEmpty()
                && project != null && !project.isEmpty();
    }
}
