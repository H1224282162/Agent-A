package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.config.KnowledgeConfig;
import org.example.knowledge.chunk.Chunk;
import org.example.knowledge.chunk.TextChunker;
import org.example.knowledge.chunk.TextChunkerFactory;
import org.example.knowledge.parse.DocumentParser;
import org.example.knowledge.parse.DocumentParserFactory;
import org.example.knowledge.parse.ParseResult;
import org.example.knowledge.vector.ElasticsearchVectorStore;
import org.example.model.KnowledgeBase;
import org.example.model.KnowledgeChunk;
import org.example.model.KnowledgeDocument;
import org.example.mapper.KnowledgeDocumentMapper;
import org.example.service.IKnowledgeBaseService;
import org.example.service.IKnowledgeChunkService;
import org.example.service.IKnowledgeDocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>
 * 知识库文档表 服务实现类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Slf4j
@Service
public class KnowledgeDocumentServiceImpl extends ServiceImpl<KnowledgeDocumentMapper, KnowledgeDocument> implements IKnowledgeDocumentService {

    private final KnowledgeConfig knowledgeConfig;
    private final DocumentParserFactory parserFactory;
    private final TextChunkerFactory chunkerFactory;
    private final ElasticsearchVectorStore vectorStore;
    private final IKnowledgeBaseService knowledgeBaseService;
    private final IKnowledgeChunkService knowledgeChunkService;

    public KnowledgeDocumentServiceImpl(KnowledgeConfig knowledgeConfig,
                                        DocumentParserFactory parserFactory,
                                        TextChunkerFactory chunkerFactory,
                                        ElasticsearchVectorStore vectorStore,
                                        IKnowledgeBaseService knowledgeBaseService,
                                        IKnowledgeChunkService knowledgeChunkService) {
        this.knowledgeConfig = knowledgeConfig;
        this.parserFactory = parserFactory;
        this.chunkerFactory = chunkerFactory;
        this.vectorStore = vectorStore;
        this.knowledgeBaseService = knowledgeBaseService;
        this.knowledgeChunkService = knowledgeChunkService;
    }

    @Override
    public Page<KnowledgeDocument> pageByKbId(Long kbId, long current, long size) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(KnowledgeDocument::getKbId, kbId);
        wrapper.orderByDesc(KnowledgeDocument::getCreatedAt);
        return page(new Page<>(current, size), wrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long uploadDocument(Long kbId, MultipartFile file) {
        if (kbId == null) {
            throw new IllegalArgumentException("知识库 ID 不能为空");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        KnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (kb == null) {
            throw new IllegalArgumentException("知识库不存在: " + kbId);
        }

        String originalName = file.getOriginalFilename();
        if (!StringUtils.hasText(originalName)) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String ext = getExtension(originalName).toLowerCase();
        if (!knowledgeConfig.getAllowedExtensions().contains(ext)) {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }

        // 保存文件到本地
        String storedName = UUID.randomUUID() + "." + ext;
        Path dirPath = Paths.get(knowledgeConfig.getFileStorePath(), "kb_" + kbId);
        Path filePath = dirPath.resolve(storedName);
        try {
            Files.createDirectories(dirPath);
            file.transferTo(filePath);
        } catch (IOException e) {
            throw new RuntimeException("文件保存失败: " + originalName, e);
        }

        // 保存文档记录
        KnowledgeDocument document = new KnowledgeDocument();
        document.setKbId(kbId);
        document.setDocName(originalName);
        document.setFileType(ext);
        document.setFileSize(file.getSize());
        document.setFilePath(filePath.toString());
        document.setChunkCount(0);
        document.setStatus((byte) 0); // 待解析
        document.setCreatedAt(LocalDateTime.now());
        document.setUpdatedAt(LocalDateTime.now());
        save(document);

        // 同步解析（后续可改为异步任务）
        doParse(document);

        return document.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean removeDocument(Long id) {
        KnowledgeDocument doc = getById(id);
        if (doc == null) {
            return false;
        }

        // 删除 ES 向量
        KnowledgeBase kb = knowledgeBaseService.getById(doc.getKbId());
        if (kb != null && StringUtils.hasText(kb.getVectorIndexName())) {
            try {
                vectorStore.deleteByDocId(kb.getVectorIndexName(), id);
            } catch (Exception e) {
                // 记录日志，不影响后续删除
                log.error("删除文档 {} 的 ES 向量失败: {}", id, e.getMessage());
            }
        }

        // 删除分块记录
        knowledgeChunkService.removeByDocId(id);

        // 删除本地文件
        if (StringUtils.hasText(doc.getFilePath())) {
            try {
                Files.deleteIfExists(Paths.get(doc.getFilePath()));
            } catch (IOException e) {
                log.error("删除本地文件失败: {}", doc.getFilePath(), e);
            }
        }

        return removeById(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reparseDocument(Long id) {
        KnowledgeDocument doc = getById(id);
        if (doc == null) {
            throw new IllegalArgumentException("文档不存在: " + id);
        }
        doParse(doc);
    }

    /**
     * 执行文档解析、分块、向量化的完整流水线
     */
    private void doParse(KnowledgeDocument document) {
        Long docId = document.getId();
        Long kbId = document.getKbId();

        KnowledgeBase kb = knowledgeBaseService.getById(kbId);
        if (kb == null) {
            updateParseStatus(docId, (byte) 9, "知识库不存在");
            return;
        }

        // 更新为解析中
        updateParseStatus(docId, (byte) 1, null);

        try {
            // 1. 解析文档
            DocumentParser parser = parserFactory.getParser(document.getFileType());
            ParseResult parseResult;
            try (var inputStream = Files.newInputStream(Paths.get(document.getFilePath()))) {
                parseResult = parser.parse(inputStream);
            }

            String text = parseResult.getText();
            if (text == null || text.trim().isEmpty()) {
                updateParseStatus(docId, (byte) 9, "文档内容为空");
                return;
            }

            // 2. 文本分块
            TextChunker chunker = chunkerFactory.getChunker(kb.getChunkStrategy());
            List<Chunk> chunks = chunker.chunk(text, kb);
            if (chunks.isEmpty()) {
                updateParseStatus(docId, (byte) 9, "分块结果为空");
                return;
            }

            // 3. 删除旧分块和向量
            knowledgeChunkService.removeByDocId(docId);
            if (StringUtils.hasText(kb.getVectorIndexName())) {
                vectorStore.deleteByDocId(kb.getVectorIndexName(), docId);
            }

            // 4. 保存新分块到 MySQL
            List<KnowledgeChunk> chunkEntities = new ArrayList<>();
            for (Chunk chunk : chunks) {
                KnowledgeChunk entity = new KnowledgeChunk();
                entity.setDocId(docId);
                entity.setKbId(kbId);
                entity.setChunkIndex(chunk.getIndex());
                entity.setContent(chunk.getContent());
                entity.setTokenCount(chunk.getTokenCount());
                entity.setStatus((byte) 1);
                entity.setCreatedAt(LocalDateTime.now());
                entity.setUpdatedAt(LocalDateTime.now());

                // metadata 转 JSON
                if (chunk.getMetadata() != null && !chunk.getMetadata().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        entity.setMetadata(mapper.writeValueAsString(chunk.getMetadata()));
                    } catch (Exception e) {
                        entity.setMetadata("{}");
                    }
                } else {
                    entity.setMetadata("{}");
                }

                chunkEntities.add(entity);
            }

            // 批量保存以获取自增 ID
            knowledgeChunkService.saveBatchChunks(chunkEntities);

            // 5. 批量写入 ES
            if (StringUtils.hasText(kb.getVectorIndexName())) {
                List<String> esDocIds = vectorStore.batchAdd(kb.getVectorIndexName(), chunkEntities);
                // 更新 es_doc_id
                for (int i = 0; i < chunkEntities.size(); i++) {
                    KnowledgeChunk chunk = chunkEntities.get(i);
                    chunk.setEsDocId(esDocIds.get(i));
                    knowledgeChunkService.updateById(chunk);
                }
            }

            // 6. 更新文档状态为已完成
            document.setChunkCount(chunkEntities.size());
            document.setStatus((byte) 2);
            document.setParseMessage(null);
            document.setUpdatedAt(LocalDateTime.now());
            updateById(document);

        } catch (Exception e) {
            log.error("文档解析失败: {}", docId, e);
            updateParseStatus(docId, (byte) 9, "解析失败: " + e.getMessage());
        }
    }

    private void updateParseStatus(Long docId, byte status, String message) {
        KnowledgeDocument update = new KnowledgeDocument();
        update.setId(docId);
        update.setStatus(status);
        update.setParseMessage(message);
        update.setUpdatedAt(LocalDateTime.now());
        updateById(update);
    }

    private String getExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        return lastDot == -1 ? "" : filename.substring(lastDot + 1);
    }
}
