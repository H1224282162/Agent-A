package org.example.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * <p>
 * 知识库主表
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Getter
@Setter
@ToString
@TableName("knowledge_base")
public class KnowledgeBase implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 知识库唯一编码，用于生成 ES 索引名
     */
    @TableField("kb_code")
    private String kbCode;

    /**
     * 知识库显示名称
     */
    @TableField("kb_name")
    private String kbName;

    /**
     * 知识库描述
     */
    @TableField("description")
    private String description;

    /**
     * 向量模型名称/编码
     */
    @TableField("embedding_model")
    private String embeddingModel;

    /**
     * 对应的 ES 索引名，如 kb_order_faq_chunks
     */
    @TableField("vector_index_name")
    private String vectorIndexName;

    /**
     * 分块策略：fixed / sliding / semantic
     */
    @TableField("chunk_strategy")
    private String chunkStrategy;

    /**
     * 单个 chunk 最大字符/Token 数
     */
    @TableField("chunk_size")
    private Integer chunkSize;

    /**
     * 分块重叠长度
     */
    @TableField("chunk_overlap")
    private Integer chunkOverlap;

    /**
     * RAG 默认召回数量
     */
    @TableField("top_k")
    private Integer topK;

    /**
     * 相似度阈值，低于此值不召回
     */
    @TableField("similarity_threshold")
    private Double similarityThreshold;

    /**
     * 状态：1启用 0禁用
     */
    @TableField("status")
    private Byte status;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField("updated_at")
    private LocalDateTime updatedAt;
}
