package org.example.service;

import org.example.agent.AgentRuntime;
import org.example.agent.AgentRuntimeFactory;
import org.example.agent.SessionContext;
import org.example.config.model.ModelRegistry;
import org.example.knowledge.rag.RagAdvisor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

/**
 * 聊天服务 —— 同时支持普通对话和 Agent 模式（工具调用）。
 * <p>
 * 普通对话与 Agent 模式的模型均通过 {@link ModelRegistry} 按 modelType 动态路由，
 * 新增模型只需在 application-llm.yml 中加配置，无需改动代码。
 */
@Service
public class ChatService {

    private final ModelRegistry modelRegistry;
    private final ChatMemory chatMemory;
    private final AgentRuntimeFactory agentRuntimeFactory;

    public ChatService(ModelRegistry modelRegistry,
                       ChatMemory chatMemory,
                       AgentRuntimeFactory agentRuntimeFactory) {
        this.modelRegistry = modelRegistry;
        this.chatMemory = chatMemory;
        this.agentRuntimeFactory = agentRuntimeFactory;
    }

    /**
     * 构建普通对话客户端：直连模型 + 会话记忆，不携带任何系统提示词或工具。
     */
    private ChatClient plainChatClient(String modelType) {
        return ChatClient.builder(modelRegistry.getChatModel(modelType))
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .build();
    }

    // ═══════════════════════════════════════════════
    // 普通对话（模型直连，非 Agent 模式）
    // ═══════════════════════════════════════════════

    public String chatSync(String sessionId, String modelType, String userMessage) {
        return plainChatClient(modelType).prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .call()
                .content();
    }

    public Flux<String> chatStream(String sessionId, String modelType, String userMessage) {
        return plainChatClient(modelType).prompt()
                .user(userMessage)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, sessionId))
                .stream()
                .content();
    }

    // ═══════════════════════════════════════════════
    // Agent 模式 — 从 DB 动态加载
    // ═══════════════════════════════════════════════

    /**
     * Agent 同步调用 —— 根据 agentCode 从数据库加载 Agent 定义并执行。
     *
     * @param agentCode  Agent 唯一编码，如 "order_helper"、"ops_helper"
     * @param sessionId  会话 ID
     * @param userMessage 用户消息
     * @return Agent 回复文本
     */
    public String agentChatSync(String agentCode, String sessionId, String userMessage) {
        AgentRuntime agent = agentRuntimeFactory.get(agentCode);

        SessionContext.setCurrentSessionId(sessionId);
        try {
            return agent.chatClient().prompt()
                    .user(userMessage)
                    .advisors(spec -> spec
                            .param(ChatMemory.CONVERSATION_ID, sessionId)
                            .param(RagAdvisor.AGENT_CODE_KEY, agentCode)
                    )
                    .call()
                    .content();
        } finally {
            SessionContext.clear();
        }
    }

    public String agentChatSync(String agentCode, String sessionId, String userMessage, String modelType) {
        AgentRuntime agent = agentRuntimeFactory.get(agentCode, modelType);

        SessionContext.setCurrentSessionId(sessionId);
        try {
            return agent.chatClient().prompt()
                    .user(userMessage)
                    .advisors(spec -> spec
                            .param(ChatMemory.CONVERSATION_ID, sessionId)
                            .param(RagAdvisor.AGENT_CODE_KEY, agentCode)
                    )
                    .call()
                    .content();
        } finally {
            SessionContext.clear();
        }
    }

    /**
     * Agent 流式调用。
     */
    public Flux<String> agentChatStream(String agentCode, String sessionId, String userMessage) {
        AgentRuntime agent = agentRuntimeFactory.get(agentCode);

        SessionContext.setCurrentSessionId(sessionId);
        return agent.chatClient().prompt()
                .user(userMessage)
                .advisors(spec -> spec
                        .param(ChatMemory.CONVERSATION_ID, sessionId)
                        .param(RagAdvisor.AGENT_CODE_KEY, agentCode)
                )
                .stream()
                .content()
                .doFinally(signal -> SessionContext.clear());
    }
}
