package org.example.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.example.dto.KnowledgeBaseSaveDTO;
import org.example.model.KnowledgeBase;

/**
 * <p>
 * 知识库主表 服务类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
public interface IKnowledgeBaseService extends IService<KnowledgeBase> {

    /**
     * 保存或更新知识库
     *
     * @param dto 保存请求 DTO
     * @return 知识库 ID
     */
    Long saveOrUpdateKb(KnowledgeBaseSaveDTO dto);

    /**
     * 删除知识库（级联删除文档、分块、ES 索引）
     *
     * @param id 知识库 ID
     * @return true 删除成功
     */
    boolean removeKb(Long id);

    /**
     * 生成 ES 索引名
     *
     * @param kbCode 知识库编码
     * @return 索引名
     */
    String generateIndexName(String kbCode);

}
