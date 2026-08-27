package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.Result;
import org.example.model.ToolDef;
import org.example.service.IToolDefService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 工具定义管理接口 —— 只读 + 状态切换（工具由 ToolScanner 自动同步）。
 */
@RestController
@RequestMapping("/toolDef")
public class ToolDefController {

    private final IToolDefService toolDefService;

    public ToolDefController(IToolDefService toolDefService) {
        this.toolDefService = toolDefService;
    }

    /** 分页列表 */
    @GetMapping("/list")
    public Result<Page<ToolDef>> list(@RequestParam(defaultValue = "1") int page,
                                      @RequestParam(defaultValue = "10") int size,
                                      @RequestParam(required = false) String category) {
        LambdaQueryWrapper<ToolDef> qw = new LambdaQueryWrapper<>();
        if (category != null && !category.isBlank()) {
            qw.eq(ToolDef::getCategory, category);
        }
        qw.orderByAsc(ToolDef::getCategory).orderByAsc(ToolDef::getToolName);
        return Result.success(toolDefService.page(Page.of(page, size), qw));
    }

    /** 获取所有启用的工具（供 Agent 管理页勾选用） */
    @GetMapping("/allEnabled")
    public Result<List<ToolDef>> allEnabled() {
        return Result.success(toolDefService.list(
                new LambdaQueryWrapper<ToolDef>().eq(ToolDef::getStatus, (byte) 1)
        ));
    }

    /** 详情 */
    @GetMapping("/{id}")
    public Result<ToolDef> getById(@PathVariable Long id) {
        return Result.success(toolDefService.getById(id));
    }

    /** 切换启用/禁用状态 */
    @PutMapping("/{id}/toggle")
    public Result<Void> toggle(@PathVariable Long id) {
        ToolDef tool = toolDefService.getById(id);
        if (tool == null) return Result.fail("工具不存在");
        tool.setStatus(tool.getStatus() == 1 ? (byte) 0 : (byte) 1);
        boolean ok = toolDefService.updateById(tool);
        return ok ? Result.success("状态切换成功", null) : Result.fail("状态切换失败");
    }
}
