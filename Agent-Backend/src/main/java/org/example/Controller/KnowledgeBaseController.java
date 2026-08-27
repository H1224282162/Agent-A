package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.Result;
import org.example.dto.KnowledgeBaseSaveDTO;
import org.example.model.KnowledgeBase;
import org.example.service.IKnowledgeBaseService;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * <p>
 * 知识库主表 前端控制器
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@RestController
@RequestMapping("/knowledgeBase")
public class KnowledgeBaseController {

    private final IKnowledgeBaseService knowledgeBaseService;

    public KnowledgeBaseController(IKnowledgeBaseService knowledgeBaseService) {
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 分页列表
     */
    @GetMapping("/list")
    public Result<Page<KnowledgeBase>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String keyword
    ) {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(KnowledgeBase::getKbCode, keyword)
                    .or()
                    .like(KnowledgeBase::getKbName, keyword);
        }
        wrapper.orderByDesc(KnowledgeBase::getUpdatedAt);
        return Result.success(knowledgeBaseService.page(new Page<>(page, size), wrapper));
    }

    /**
     * 详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeBase> detail(@PathVariable Long id) {
        KnowledgeBase kb = knowledgeBaseService.getById(id);
        if (kb == null) {
            return Result.fail("知识库不存在");
        }
        return Result.success(kb);
    }

    /**
     * 新增
     */
    @PostMapping
    public Result<Long> save(@RequestBody KnowledgeBaseSaveDTO dto) {
        Long id = knowledgeBaseService.saveOrUpdateKb(dto);
        return Result.success("创建成功", id);
    }

    /**
     * 编辑
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody KnowledgeBaseSaveDTO dto) {
        dto.setId(id);
        knowledgeBaseService.saveOrUpdateKb(dto);
        return Result.success("更新成功", null);
    }

    /**
     * 删除
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = knowledgeBaseService.removeKb(id);
        return success ? Result.success("删除成功", null) : Result.fail("知识库不存在");
    }

    /**
     * 所有启用的知识库（供下拉框）
     */
    @GetMapping("/allEnabled")
    public Result<List<KnowledgeBase>> allEnabled() {
        LambdaQueryWrapper<KnowledgeBase> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeBase::getStatus, (byte) 1);
        wrapper.orderByDesc(KnowledgeBase::getUpdatedAt);
        return Result.success(knowledgeBaseService.list(wrapper));
    }

}
