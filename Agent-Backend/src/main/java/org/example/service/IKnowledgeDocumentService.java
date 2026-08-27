package org.example.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.IService;
import org.example.model.KnowledgeDocument;
import org.springframework.web.multipart.MultipartFile;

/**
 * <p>
 * 知识库文档表 服务类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
public interface IKnowledgeDocumentService extends IService<KnowledgeDocument> {

    /**
     * 分页查询某知识库下的文档
     *
     * @param kbId    知识库 ID
     * @param current 当前页
     * @param size    每页大小
     * @return 分页结果
     */
    Page<KnowledgeDocument> pageByKbId(Long kbId, long current, long size);

    /**
     * 上传文档并触发解析
     *
     * @param kbId 知识库 ID
     * @param file 上传文件
     * @return 文档 ID
     */
    Long uploadDocument(Long kbId, MultipartFile file);

    /**
     * 删除文档（级联删除分块、向量、本地文件）
     *
     * @param id 文档 ID
     * @return true 删除成功
     */
    boolean removeDocument(Long id);

    /**
     * 重新解析文档
     *
     * @param id 文档 ID
     */
    void reparseDocument(Long id);

}
