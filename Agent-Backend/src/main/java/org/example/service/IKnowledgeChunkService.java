package org.example.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.example.model.KnowledgeChunk;

import java.util.List;

/**
 * <p>
 * 知识库文本分块表 服务类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
public interface IKnowledgeChunkService extends IService<KnowledgeChunk> {

    /**
     * 批量保存分块
     *
     * @param chunks 分块列表
     */
    void saveBatchChunks(List<KnowledgeChunk> chunks);

    /**
     * 根据文档 ID 删除分块
     *
     * @param docId 文档 ID
     */
    void removeByDocId(Long docId);

}
