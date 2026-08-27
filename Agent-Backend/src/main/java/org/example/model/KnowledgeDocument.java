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
 * 知识库文档表
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Getter
@Setter
@ToString
@TableName("knowledge_document")
public class KnowledgeDocument implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键ID
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 所属知识库 ID
     */
    @TableField("kb_id")
    private Long kbId;

    /**
     * 文档原始文件名
     */
    @TableField("doc_name")
    private String docName;

    /**
     * 文件类型：pdf / word / txt / markdown
     */
    @TableField("file_type")
    private String fileType;

    /**
     * 文件大小（字节）
     */
    @TableField("file_size")
    private Long fileSize;

    /**
     * 文件存储路径（本地或 OSS）
     */
    @TableField("file_path")
    private String filePath;

    /**
     * 文档来源链接（可选）
     */
    @TableField("source_url")
    private String sourceUrl;

    /**
     * 已生成的 chunk 数量
     */
    @TableField("chunk_count")
    private Integer chunkCount;

    /**
     * 解析状态：0待解析 1解析中 2已完成 9失败
     */
    @TableField("status")
    private Byte status;

    /**
     * 解析失败时的错误信息
     */
    @TableField("parse_message")
    private String parseMessage;

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
