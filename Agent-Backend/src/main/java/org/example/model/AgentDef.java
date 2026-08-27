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
 * 
 * </p>
 *
 * @author wzh
 * @since 2026-08-08
 */
@Getter
@Setter
@ToString
@TableName("agent_def")
public class AgentDef implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 唯一标识: "order_helper"
     */
    @TableField("agent_code")
    private String agentCode;

    /**
     * 显示名: "蜜雪冰城点单助手"
     */
    @TableField("agent_name")
    private String agentName;

    /**
     * 显示名: "蜜雪冰城点单助手"
     */
    @TableField("system_prompt")
    private String systemPrompt;

    /**
     * 默认模型
     */
    @TableField("model_type")
    private String modelType;

    /**
     * 1启用 0禁用 2草稿
     */
    @TableField("status")
    private Byte status;

    /**
     * 版本号
     */
    @TableField("version")
    private Integer version;

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
