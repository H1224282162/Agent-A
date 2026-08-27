package org.example.config.model;

import lombok.Data;
import org.example.common.ChatMessageVO;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 大模型配置 —— 对应 application-llm.yml 中 {@code app.models} 前缀。
 * <p>
 * 对话模型与向量模型统一用列表组织，新增模型只需在配置里加一条，实现「加模型不改代码」。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.models")
public class ModelProperties {

    /**
     * 对话模型列表
     */
    private List<ChatModelConfig> chat = new ArrayList<>();

    /**
     * 向量（Embedding）模型列表
     */
    private List<EmbeddingModelConfig> embedding = new ArrayList<>();

    /**
     * 对话模型配置
     */
    @Data
    public static class ChatModelConfig {
        /** 路由 key，对应 agent_def.model_type（如 deepseek、kimi） */
        private String name;
        /** OpenAI 兼容 API 地址 */
        private String baseUrl;
        /** API Key */
        private String apiKey;
        /** 模型名（如 deepseek-chat、kimi-k3） */
        private String model;
        /** 温度 */
        private Double temperature;
        /** 最大 token 数 */
        private Integer maxTokens;
    }

    /**
     * 向量模型配置
     */
    @Data
    public static class EmbeddingModelConfig {
        /** 路由 key（如 bge-m3） */
        private String name;
        /** OpenAI 兼容 API 地址 */
        private String baseUrl;
        /** API Key */
        private String apiKey;
        /** 模型名（如 BAAI/bge-m3） */
        private String model;
    }
}
