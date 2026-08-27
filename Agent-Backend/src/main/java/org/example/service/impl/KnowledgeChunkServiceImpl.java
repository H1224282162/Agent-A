package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.model.KnowledgeChunk;
import org.example.mapper.KnowledgeChunkMapper;
import org.example.service.IKnowledgeChunkService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * <p>
 * 知识库文本分块表 服务实现类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Service
public class KnowledgeChunkServiceImpl extends ServiceImpl<KnowledgeChunkMapper, KnowledgeChunk> implements IKnowledgeChunkService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveBatchChunks(List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return;
        }
        saveBatch(chunks, 200);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByDocId(Long docId) {
        LambdaQueryWrapper<KnowledgeChunk> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeChunk::getDocId, docId);
        remove(wrapper);
    }

}
