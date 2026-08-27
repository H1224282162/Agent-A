package org.example.dto;

import lombok.Data;

/**
 * 知识库保存请求 DTO
 */
@Data
public class KnowledgeBaseSaveDTO {

    /**
     * 主键 ID（编辑时传入）
     */
    private Long id;

    /**
     * 知识库唯一编码
     */
    private String kbCode;

    /**
     * 知识库显示名称
     */
    private String kbName;

    /**
     * 描述
     */
    private String description;

    /**
     * 向量模型名称
     */
    private String embeddingModel;

    /**
     * 分块策略：fixed / sliding / paragraph
     */
    private String chunkStrategy;

    /**
     * 单个 chunk 最大字符数
     */
    private Integer chunkSize;

    /**
     * 分块重叠长度
     */
    private Integer chunkOverlap;

    /**
     * RAG 默认召回数量
     */
    private Integer topK;

    /**
     * 相似度阈值
     */
    private Double similarityThreshold;

    /**
     * 状态：1启用 0禁用
     */
    private Byte status;

}
