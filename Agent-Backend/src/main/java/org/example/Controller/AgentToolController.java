package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.model.AgentTool;
import org.example.service.IAgentToolService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Agent-工具关联管理接口。
 */
@RestController
@RequestMapping("/agentTool")
public class AgentToolController {

    private final IAgentToolService agentToolService;

    public AgentToolController(IAgentToolService agentToolService) {
        this.agentToolService = agentToolService;
    }

    /** 获取某个 Agent 绑定的所有工具 */
    @GetMapping("/list")
    public Result<List<AgentTool>> list(@RequestParam Long agentId) {
        return Result.success(agentToolService.list(
                new LambdaQueryWrapper<AgentTool>().eq(AgentTool::getAgentId, agentId)
        ));
    }

    /** 绑定工具 */
    @PostMapping
    public Result<Void> bind(@RequestBody AgentTool entity) {
        // 防重复
        boolean exists = agentToolService.count(new LambdaQueryWrapper<AgentTool>()
                .eq(AgentTool::getAgentId, entity.getAgentId())
                .eq(AgentTool::getToolId, entity.getToolId())) > 0;
        if (exists) return Result.fail("该工具已绑定");
        if (entity.getEnabled() == null) entity.setEnabled((byte) 1);
        boolean ok = agentToolService.save(entity);
        return ok ? Result.success("绑定成功", null) : Result.fail("绑定失败");
    }

    /** 解除绑定 */
    @DeleteMapping
    public Result<Void> unbind(@RequestParam Long agentId, @RequestParam Long toolId) {
        boolean ok = agentToolService.remove(new LambdaQueryWrapper<AgentTool>()
                .eq(AgentTool::getAgentId, agentId)
                .eq(AgentTool::getToolId, toolId));
        return ok ? Result.success("解绑成功", null) : Result.fail("解绑失败");
    }

    /** 切换某个绑定的启用/禁用 */
    @PutMapping("/toggle")
    public Result<Void> toggle(@RequestParam Long agentId, @RequestParam Long toolId) {
        AgentTool at = agentToolService.getOne(new LambdaQueryWrapper<AgentTool>()
                .eq(AgentTool::getAgentId, agentId)
                .eq(AgentTool::getToolId, toolId));
        if (at == null) return Result.fail("绑定关系不存在");
        at.setEnabled(at.getEnabled() == 1 ? (byte) 0 : (byte) 1);
        boolean ok = agentToolService.updateById(at);
        return ok ? Result.success("切换成功", null) : Result.fail("切换失败");
    }
}
