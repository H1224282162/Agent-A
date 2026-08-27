package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.Result;
import org.example.model.AgentPromptHistory;
import org.example.service.IAgentPromptHistoryService;
import org.springframework.web.bind.annotation.*;

/**
 * Prompt 版本历史 —— 只读接口。
 */
@RestController
@RequestMapping("/agentPromptHistory")
public class AgentPromptHistoryController {

    private final IAgentPromptHistoryService historyService;

    public AgentPromptHistoryController(IAgentPromptHistoryService historyService) {
        this.historyService = historyService;
    }

    /** 按 Agent 查看版本历史 */
    @GetMapping("/list")
    public Result<Page<AgentPromptHistory>> list(@RequestParam(defaultValue = "1") int page,
                                                 @RequestParam(defaultValue = "10") int size,
                                                 @RequestParam Long agentId) {
        LambdaQueryWrapper<AgentPromptHistory> qw = new LambdaQueryWrapper<>();
        qw.eq(AgentPromptHistory::getAgentId, agentId)
                .orderByDesc(AgentPromptHistory::getVersion);
        return Result.success(historyService.page(Page.of(page, size), qw));
    }
}
