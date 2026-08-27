package org.example.config.model;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 模型注册表 —— 多模型动态路由核心。
 * <p>
 * 应用启动时根据 {@link ModelProperties} 批量构建 {@link ChatModel} 与 {@link EmbeddingModel}，
 * 之后按 {@code name} 路由。新增模型只需在 application-llm.yml 中加一条配置，无需改动 Java 代码。
 * <p>
 * 所有对话模型统一走 OpenAI 兼容协议（DeepSeek / Kimi / 硅基流动等），
 * 仅通过 base-url、api-key、model 三个字段区分。
 */
@Component
public class ModelRegistry {

    /** 对话模型注册表：name → ChatModel */
    private final Map<String, ChatModel> chatModels = new LinkedHashMap<>();

    /** 向量模型注册表：name → EmbeddingModel */
    private final Map<String, EmbeddingModel> embeddingModels = new LinkedHashMap<>();

    /** 默认对话模型名 */
    private final String defaultChatModelName;

    /** 默认向量模型名 */
    private final String defaultEmbeddingModelName;

    public ModelRegistry(ModelProperties properties,
                         ToolCallingManager toolCallingManager,
                         RetryTemplate retryTemplate) {
        for (ModelProperties.ChatModelConfig c : properties.getChat()) {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(c.getBaseUrl())
                    .apiKey(c.getApiKey())
                    .build();
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(c.getModel())
                    .temperature(c.getTemperature())
                    .maxTokens(c.getMaxTokens())
                    .build();
            chatModels.put(c.getName(), new OpenAiChatModel(
                    api, options, toolCallingManager, retryTemplate, ObservationRegistry.NOOP));
        }
        for (ModelProperties.EmbeddingModelConfig c : properties.getEmbedding()) {
            OpenAiApi api = OpenAiApi.builder()
                    .baseUrl(c.getBaseUrl())
                    .apiKey(c.getApiKey())
                    .build();
            OpenAiEmbeddingOptions options = OpenAiEmbeddingOptions.builder()
                    .model(c.getModel())
                    .build();
            embeddingModels.put(c.getName(), new OpenAiEmbeddingModel(api, MetadataMode.EMBED, options));
        }
        this.defaultChatModelName = chatModels.isEmpty() ? "deepseek" : chatModels.keySet().iterator().next();
        this.defaultEmbeddingModelName = embeddingModels.isEmpty() ? "bge-m3" : embeddingModels.keySet().iterator().next();
    }

    /**
     * 按 name 获取对话模型。
     *
     * @param name 模型名；为空时回退默认模型
     * @throws IllegalArgumentException 未配置该模型
     */
    public ChatModel getChatModel(String name) {
        String key = (name == null || name.isBlank()) ? defaultChatModelName : name.trim();
        ChatModel model = chatModels.get(key);
        if (model == null) {
            throw new IllegalArgumentException("未配置的对话模型: " + key + "，可选: " + chatModels.keySet());
        }
        return model;
    }

    /**
     * 按 name 获取向量模型。
     *
     * @param name 模型名；为空时回退默认模型
     * @throws IllegalArgumentException 未配置该模型
     */
    public EmbeddingModel getEmbeddingModel(String name) {
        String key = (name == null || name.isBlank()) ? defaultEmbeddingModelName : name.trim();
        EmbeddingModel model = embeddingModels.get(key);
        if (model == null) {
            throw new IllegalArgumentException("未配置的向量模型: " + key + "，可选: " + embeddingModels.keySet());
        }
        return model;
    }

    /**
     * 获取默认向量模型（当前为 bge-m3）。
     */
    public EmbeddingModel defaultEmbeddingModel() {
        return getEmbeddingModel(null);
    }
}
