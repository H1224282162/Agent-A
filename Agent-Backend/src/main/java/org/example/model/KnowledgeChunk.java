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
 * 知识库文本分块表
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Getter
@Setter
@ToString
@TableName("knowledge_chunk")
public class KnowledgeChunk implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属文档 ID
     */
    @TableField("doc_id")
    private Long docId;

    /**
     * 所属知识库 ID（冗余字段，方便按知识库清理）
     */
    @TableField("kb_id")
    private Long kbId;

    /**
     * 文档内 chunk 序号
     */
    @TableField("chunk_index")
    private Integer chunkIndex;

    /**
     * chunk 文本内容（展示/溯源用）
     */
    @TableField("content")
    private String content;

    /**
     * 元数据：页码、标题、段落等
     */
    @TableField("metadata")
    private String metadata;

    /**
     * Token 数量估算
     */
    @TableField("token_count")
    private Integer tokenCount;

    /**
     * 对应 ES 文档 ID，用于更新/删除向量
     */
    @TableField("es_doc_id")
    private String esDocId;

    /**
     * 状态：1有效 0无效
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
