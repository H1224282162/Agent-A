package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.example.common.Result;
import org.example.model.KnowledgeChunk;
import org.example.service.IKnowledgeChunkService;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 知识库文本分块表 前端控制器
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@RestController
@RequestMapping("/knowledgeChunk")
public class KnowledgeChunkController {

    private final IKnowledgeChunkService knowledgeChunkService;

    public KnowledgeChunkController(IKnowledgeChunkService knowledgeChunkService) {
        this.knowledgeChunkService = knowledgeChunkService;
    }

    /**
     * 分块列表
     */
    @GetMapping("/list")
    public Result<Page<KnowledgeChunk>> list(
            @RequestParam Long docId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocId, docId);
        wrapper.orderByAsc(KnowledgeChunk::getChunkIndex);
        return Result.success(knowledgeChunkService.page(new Page<>(page, size), wrapper));
    }

    /**
     * 分块详情
     */
    @GetMapping("/{id}")
    public Result<KnowledgeChunk> detail(@PathVariable Long id) {
        KnowledgeChunk chunk = knowledgeChunkService.getById(id);
        if (chunk == null) {
            return Result.fail("分块不存在");
        }
        return Result.success(chunk);
    }

}
