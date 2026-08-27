package org.example.agent;

import java.util.List;

/**
 * Agent 定义的数据载体 —— 对应 agent_def 表的一行记录。
 * <p>
 * 从数据库加载后用于组装 {@link AgentRuntime}。
 *
 * @param agentCode    Agent 唯一标识，如 "order_helper"
 * @param agentName    Agent 显示名称，如 "蜜雪冰城点单助手"
 * @param systemPrompt 完整的 System Prompt
 * @param modelType    默认模型类型: deepseek / kimi
 * @param version      当前 Prompt 版本号
 * @param toolNames    绑定的工具方法名列表，如 ["getMyLocation", "login", ...]
 */
public record AgentDefinition(
        String agentCode,
        String agentName,
        String systemPrompt,
        String modelType,
        int version,
        List<String> toolNames
) {}
