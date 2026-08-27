package org.example.knowledge.chunk;

import org.example.model.KnowledgeBase;

import java.util.List;

/**
 * 文本分块器接口
 */
public interface TextChunker {

    /**
     * 是否支持该分块策略
     *
     * @param strategy 分块策略名称
     * @return true 表示支持
     */
    boolean supports(String strategy);

    /**
     * 对文本进行分块
     *
     * @param text 原始文本
     * @param kb   知识库配置（包含 chunkSize、chunkOverlap 等）
     * @return 分块列表
     */
    List<Chunk> chunk(String text, KnowledgeBase kb);
}
