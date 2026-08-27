package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.Result;
import org.example.model.AgentCallLog;
import org.example.service.IAgentCallLogService;
import org.springframework.web.bind.annotation.*;

/**
 * 调用日志接口 —— 只读 + 删除，不提供新增/修改。
 */
@RestController
@RequestMapping("/agentCallLog")
public class AgentCallLogController {

    private final IAgentCallLogService callLogService;

    public AgentCallLogController(IAgentCallLogService callLogService) {
        this.callLogService = callLogService;
    }

    /** 分页列表 */
    @GetMapping("/list")
    public Result<Page<AgentCallLog>> list(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "10") int size,
                                           @RequestParam(required = false) Long agentId) {
        LambdaQueryWrapper<AgentCallLog> qw = new LambdaQueryWrapper<>();
        if (agentId != null) qw.eq(AgentCallLog::getAgentId, agentId);
        qw.orderByDesc(AgentCallLog::getCreatedAt);
        return Result.success(callLogService.page(Page.of(page, size), qw));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<AgentCallLog> getById(@PathVariable Long id) {
        return Result.success(callLogService.getById(id));
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = callLogService.removeById(id);
        return ok ? Result.success("删除成功", null) : Result.fail("删除失败");
    }
}
