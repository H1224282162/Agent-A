package org.example.Controller;

import org.example.agent.AgentRuntime;
import org.example.agent.AgentRuntimeFactory;
import org.example.common.Result;
import org.example.service.ChatService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 通用 Agent 控制器 —— 一个端点支持所有 Agent。
 * <p>
 * 从"一个接口对应一个 Agent"升级为通用入口：
 * {@code /agent/{agentCode}/chat} —— agentCode 对应数据库中的 agent_def.agent_code。
 */
@RestController
public class AgentController {

    private final ChatService chatService;
    private final AgentRuntimeFactory agentRuntimeFactory;

    public AgentController(ChatService chatService, AgentRuntimeFactory agentRuntimeFactory) {
        this.chatService = chatService;
        this.agentRuntimeFactory = agentRuntimeFactory;
    }

    /**
     * 列出所有已加载的 Agent。
     */
    @GetMapping("/agent/list")
    public Result<List<Map<String, Object>>> listAgents() {
        List<Map<String, Object>> list = agentRuntimeFactory.listAgentCodes().stream()
                .map(code -> {
                    AgentRuntime rt = agentRuntimeFactory.get(code);
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("agentCode", rt.agentCode());
                    m.put("agentName", rt.agentName());
                    m.put("version", rt.version());
                    m.put("modelType", rt.modelType());
                    m.put("toolNames", rt.toolNames());
                    return m;
                })
                .toList();
        return Result.success(list);
    }

    /**
     * 热加载指定 Agent（重新从数据库加载 Prompt 和工具绑定）。
     */
    @GetMapping("/agent/{agentCode}/reload")
    public Result<Map<String, Object>> reload(@PathVariable String agentCode) {
        AgentRuntime rt = agentRuntimeFactory.reload(agentCode);
        if (rt == null) {
            return Result.fail("Agent 不存在或已禁用: " + agentCode);
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("agentCode", rt.agentCode());
        m.put("agentName", rt.agentName());
        m.put("version", rt.version());
        m.put("toolNames", rt.toolNames());
        return Result.success("热加载成功", m);
    }

    /**
     * Agent 同步调用 —— 通用入口。
     *
     * @param agentCode Agent 编码，如 "order_helper"、"ops_helper"
     * @param sessionId 会话 ID，同一会话多轮对话使用相同 sessionId
     * @param msg       用户输入的自然语言消息
     * @param modelType 可选，指定本次调用的大模型类型（如 deepseek/kimi）；为空时使用 Agent 默认模型
     * @return Agent 的最终回复文本
     */
    @GetMapping("/agent/{agentCode}/chat")
    public Result<String> agentChat(
            @PathVariable String agentCode,
            @RequestParam String sessionId,
            @RequestParam String msg,
            @RequestParam(required = false) String modelType
    ) {
        try {
            String reply = chatService.agentChatSync(agentCode, sessionId, msg, modelType);
            return Result.success(reply);
        } catch (IllegalArgumentException e) {
            return Result.fail(e.getMessage());
        } catch (Exception e) {
            return Result.fail("Agent 调用失败: " + e.getMessage());
        }
    }

    /**
     * Agent 流式调用（SSE）。
     * <p>
     * 注意：SSE 流式接口无法用 Result 包装，直接返回 Flux<String>。
     */
    @GetMapping(value = "/agent/{agentCode}/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> agentChatStream(
            @PathVariable String agentCode,
            @RequestParam String sessionId,
            @RequestParam String msg
    ) {
        return chatService.agentChatStream(agentCode, sessionId, msg);
    }
}
