package org.example.Controller;

import org.example.common.ChatMessageVO;
import org.example.common.Result;
import org.example.service.SessionService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 会话管理控制器 —— 提供会话 id 生成、历史会话列表、历史消息查询。
 */
@RestController
@RequestMapping("/session")
public class SessionController {

    private final SessionService sessionService;

    public SessionController(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * 生成新会话 id。
     *
     * @param agentCode Agent 编码
     * @return 新会话 id
     */
    @GetMapping("/new")
    public Result<String> newSession(@RequestParam String agentCode) {
        return Result.success(sessionService.newSessionId(agentCode));
    }

    /**
     * 查询某 Agent 的历史会话 id 列表（按时间倒序）。
     */
    @GetMapping("/list")
    public Result<List<String>> listSessions(@RequestParam String agentCode) {
        return Result.success(sessionService.listSessions(agentCode));
    }

    /**
     * 查询某会话的历史消息。
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessageVO>> history(@PathVariable String sessionId) {
        return Result.success(sessionService.history(sessionId));
    }
}
