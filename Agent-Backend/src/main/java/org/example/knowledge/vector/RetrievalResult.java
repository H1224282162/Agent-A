package org.example.knowledge.vector;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 向量检索结果
 */
@Data
public class RetrievalResult {

    /**
     * chunk 主键 ID
     */
    private Long chunkId;

    /**
     * 所属文档 ID
     */
    private Long docId;

    /**
     * 所属知识库 ID
     */
    private Long kbId;

    /**
     * chunk 文本内容
     */
    private String content;

    /**
     * 相似度分数
     */
    private Double score;

    /**
     * 元数据
     */
    private Map<String, Object> metadata = new HashMap<>();

}
