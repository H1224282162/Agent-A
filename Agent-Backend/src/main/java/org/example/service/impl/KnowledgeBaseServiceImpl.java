package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.config.KnowledgeConfig;
import org.example.dto.KnowledgeBaseSaveDTO;
import org.example.knowledge.vector.ElasticsearchVectorStore;
import org.example.mapper.KnowledgeChunkMapper;
import org.example.mapper.KnowledgeDocumentMapper;
import org.example.model.KnowledgeBase;
import org.example.model.KnowledgeChunk;
import org.example.model.KnowledgeDocument;
import org.example.service.IKnowledgeBaseService;
import org.example.mapper.KnowledgeBaseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

/**
 * <p>
 * 知识库主表 服务实现类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Service
public class KnowledgeBaseServiceImpl extends ServiceImpl<KnowledgeBaseMapper, KnowledgeBase> implements IKnowledgeBaseService {

    private final KnowledgeConfig knowledgeConfig;
    private final ElasticsearchVectorStore vectorStore;
    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;

    public KnowledgeBaseServiceImpl(KnowledgeConfig knowledgeConfig,
                                    ElasticsearchVectorStore vectorStore,
                                    KnowledgeDocumentMapper documentMapper,
                                    KnowledgeChunkMapper chunkMapper) {
        this.knowledgeConfig = knowledgeConfig;
        this.vectorStore = vectorStore;
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveOrUpdateKb(KnowledgeBaseSaveDTO dto) {
        // 参数校验
        if (!StringUtils.hasText(dto.getKbCode())) {
            throw new IllegalArgumentException("知识库编码不能为空");
        }
        if (!StringUtils.hasText(dto.getKbName())) {
            throw new IllegalArgumentException("知识库名称不能为空");
        }

        // kbCode 唯一性校验
        LambdaQueryWrapper<KnowledgeBase> codeWrapper = new LambdaQueryWrapper<>();
        codeWrapper.eq(KnowledgeBase::getKbCode, dto.getKbCode());
        if (dto.getId() != null) {
            codeWrapper.ne(KnowledgeBase::getId, dto.getId());
        }
        long count = count(codeWrapper);
        if (count > 0) {
            throw new IllegalArgumentException("知识库编码已存在: " + dto.getKbCode());
        }

        KnowledgeBase entity = convertToEntity(dto);

        if (dto.getId() == null) {
            // 新增：自动生成索引名并创建 ES 索引
            entity.setVectorIndexName(generateIndexName(entity.getKbCode()));
            entity.setCreatedAt(LocalDateTime.now());
            entity.setUpdatedAt(LocalDateTime.now());
            save(entity);
            // 创建 ES 索引，维度从 EmbeddingModel 获取
            vectorStore.createIndex(entity.getVectorIndexName(), vectorStore.getDimensions());
        } else {
            // 编辑：不修改索引名和编码
            KnowledgeBase existing = getById(dto.getId());
            if (existing == null) {
                throw new IllegalArgumentException("知识库不存在: " + dto.getId());
            }
            entity.setVectorIndexName(existing.getVectorIndexName());
            entity.setKbCode(existing.getKbCode());
            entity.setCreatedAt(existing.getCreatedAt());
            entity.setUpdatedAt(LocalDateTime.now());
            updateById(entity);
        }

        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeKb(Long id) {
        KnowledgeBase kb = getById(id);
        if (kb == null) {
            return false;
        }

        // 1. 删除该知识库下所有文档的分块记录和 ES 向量
        LambdaQueryWrapper<KnowledgeDocument> docWrapper = new LambdaQueryWrapper<>();
        docWrapper.eq(KnowledgeDocument::getKbId, id);
        List<KnowledgeDocument> documents = documentMapper.selectList(docWrapper);
        for (KnowledgeDocument doc : documents) {
            // 删除分块
            LambdaQueryWrapper<KnowledgeChunk> chunkWrapper = new LambdaQueryWrapper<>();
            chunkWrapper.eq(KnowledgeChunk::getDocId, doc.getId());
            chunkMapper.delete(chunkWrapper);
        }

        // 2. 删除文档记录
        documentMapper.delete(docWrapper);

        // 3. 删除 ES 索引
        if (StringUtils.hasText(kb.getVectorIndexName())) {
            vectorStore.deleteIndex(kb.getVectorIndexName());
        }

        // 4. 删除知识库
        return removeById(id);
    }

    @Override
    public String generateIndexName(String kbCode) {
        String prefix = knowledgeConfig.getIndexPrefix();
        return prefix + "_" + kbCode.toLowerCase().replaceAll("[^a-z0-9]", "_") + "_chunks";
    }

    private KnowledgeBase convertToEntity(KnowledgeBaseSaveDTO dto) {
        KnowledgeBase entity = new KnowledgeBase();
        entity.setId(dto.getId());
        entity.setKbCode(dto.getKbCode().trim());
        entity.setKbName(dto.getKbName().trim());
        entity.setDescription(dto.getDescription());
        entity.setEmbeddingModel(StringUtils.hasText(dto.getEmbeddingModel()) ? dto.getEmbeddingModel() : "bge-m3");
        entity.setChunkStrategy(StringUtils.hasText(dto.getChunkStrategy()) ? dto.getChunkStrategy() : "fixed");
        entity.setChunkSize(dto.getChunkSize() != null ? dto.getChunkSize() : 512);
        entity.setChunkOverlap(dto.getChunkOverlap() != null ? dto.getChunkOverlap() : 50);
        entity.setTopK(dto.getTopK() != null ? dto.getTopK() : 5);
        entity.setSimilarityThreshold(dto.getSimilarityThreshold() != null ? dto.getSimilarityThreshold() : 0.75);
        entity.setStatus(dto.getStatus() != null ? dto.getStatus() : (byte) 1);
        return entity;
    }
}
