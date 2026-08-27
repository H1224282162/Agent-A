package org.example.agent;

import org.springframework.ai.chat.client.ChatClient;

import java.util.Collections;
import java.util.List;

/**
 * Agent 运行时对象 —— 一个已经完全组装好的、可直接执行对话的 Agent 实例。
 * <p>
 * 封装了 ChatClient（已绑定 System Prompt + Tools + Advisor）以及元数据，
 * 由 {@link AgentRuntimeFactory} 在启动时从数据库加载组装，缓存到内存。
 */
public class AgentRuntime {

    /** Agent 元数据 */
    private final AgentDefinition definition;

    /** 已组装好 System Prompt + Tools + Advisor 的 ChatClient */
    private final ChatClient chatClient;

    /** 加载时间戳 */
    private final long loadedAt;

    public AgentRuntime(AgentDefinition definition, ChatClient chatClient) {
        this.definition = definition;
        this.chatClient = chatClient;
        this.loadedAt = System.currentTimeMillis();
    }

    // ──────────── 便捷 getter ────────────

    public String agentCode()   { return definition.agentCode(); }
    public String agentName()   { return definition.agentName(); }
    public int version()        { return definition.version(); }
    public String modelType()   { return definition.modelType(); }
    public long loadedAt()      { return loadedAt; }
    public List<String> toolNames() { return definition.toolNames() != null
            ? Collections.unmodifiableList(definition.toolNames()) : List.of(); }

    /** 获取底层的 ChatClient，直接用于对话。 */
    public ChatClient chatClient() { return chatClient; }

    @Override
    public String toString() {
        return "AgentRuntime{code='" + agentCode() + "', name='" + agentName()
                + "', version=" + version() + ", tools=" + toolNames() + "}";
    }
}
