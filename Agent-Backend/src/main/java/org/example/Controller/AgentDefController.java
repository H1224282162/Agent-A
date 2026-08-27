package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.Result;
import org.example.model.AgentDef;
import org.example.service.IAgentDefService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

/**
 * Agent 定义管理接口 —— 前端后台 CRUD。
 */
@RestController
@RequestMapping("/agentDef")
public class AgentDefController {

    private final IAgentDefService agentDefService;

    public AgentDefController(IAgentDefService agentDefService) {
        this.agentDefService = agentDefService;
    }

    /** 分页列表 */
    @GetMapping("/list")
    public Result<Page<AgentDef>> list(@RequestParam(defaultValue = "1") int page,
                                       @RequestParam(defaultValue = "10") int size,
                                       @RequestParam(required = false) String keyword) {
        LambdaQueryWrapper<AgentDef> qw = new LambdaQueryWrapper<>();
        if (keyword != null && !keyword.isBlank()) {
            qw.and(w -> w.like(AgentDef::getAgentCode, keyword)
                    .or().like(AgentDef::getAgentName, keyword));
        }
        qw.orderByDesc(AgentDef::getUpdatedAt);
        return Result.success(agentDefService.page(Page.of(page, size), qw));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<AgentDef> getById(@PathVariable Long id) {
        return Result.success(agentDefService.getById(id));
    }

    /** 新增 */
    @PostMapping
    public Result<Long> save(@RequestBody AgentDef entity) {
        entity.setCreatedAt(LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        if (entity.getVersion() == null) entity.setVersion(1);
        if (entity.getStatus() == null) entity.setStatus((byte) 1);
        boolean ok = agentDefService.save(entity);
        return ok ? Result.success("创建成功", entity.getId()) : Result.fail("创建失败");
    }

    /** 更新 */
    @PutMapping
    public Result<Void> update(@RequestBody AgentDef entity) {
        entity.setUpdatedAt(LocalDateTime.now());
        boolean ok = agentDefService.updateById(entity);
        return ok ? Result.success("更新成功", null) : Result.fail("更新失败");
    }

    /** 删除 */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean ok = agentDefService.removeById(id);
        return ok ? Result.success("删除成功", null) : Result.fail("删除失败");
    }

    /** 复制一个 Agent（用于快速创建变体） */
    @PostMapping("/{id}/clone")
    public Result<Void> clone(@PathVariable Long id) {
        AgentDef src = agentDefService.getById(id);
        if (src == null) return Result.fail("源 Agent 不存在");
        src.setId(null);
        src.setAgentCode(src.getAgentCode() + "_copy");
        src.setAgentName(src.getAgentName() + "（副本）");
        src.setVersion(1);
        src.setCreatedAt(LocalDateTime.now());
        src.setUpdatedAt(LocalDateTime.now());
        boolean ok = agentDefService.save(src);
        return ok ? Result.success("克隆成功", null) : Result.fail("克隆失败");
    }
}
