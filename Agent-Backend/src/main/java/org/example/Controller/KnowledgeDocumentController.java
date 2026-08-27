package org.example.Controller;

import org.example.common.Result;
import org.example.model.KnowledgeDocument;
import org.example.service.IKnowledgeDocumentService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 知识库文档表 前端控制器
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@RestController
@RequestMapping("/knowledgeDocument")
public class KnowledgeDocumentController {

    private final IKnowledgeDocumentService knowledgeDocumentService;

    public KnowledgeDocumentController(IKnowledgeDocumentService knowledgeDocumentService) {
        this.knowledgeDocumentService = knowledgeDocumentService;
    }

    /**
     * 文档列表
     */
    @GetMapping("/list")
    public Result<Page<KnowledgeDocument>> list(
            @RequestParam Long kbId,
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return Result.success(knowledgeDocumentService.pageByKbId(kbId, page, size));
    }

    /**
     * 上传文档
     */
    @PostMapping("/upload")
    public Result<Long> upload(
            @RequestParam Long kbId,
            @RequestParam("file") MultipartFile file
    ) {
        Long docId = knowledgeDocumentService.uploadDocument(kbId, file);
        return Result.success("上传成功", docId);
    }

    /**
     * 删除文档
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = knowledgeDocumentService.removeDocument(id);
        return success ? Result.success("删除成功", null) : Result.fail("文档不存在");
    }

    /**
     * 重新解析
     */
    @PostMapping("/{id}/reparse")
    public Result<Void> reparse(@PathVariable Long id) {
        knowledgeDocumentService.reparseDocument(id);
        return Result.success("重新解析已触发", null);
    }

}
