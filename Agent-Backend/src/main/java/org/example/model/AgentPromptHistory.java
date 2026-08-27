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
@TableName("agent_prompt_history")
public class AgentPromptHistory implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("agent_id")
    private Long agentId;

    @TableField("version")
    private Integer version;

    /**
     * 该版本的完整 Prompt
     */
    @TableField("prompt")
    private String prompt;

    /**
     * 该版本的完整 Prompt
     */
    @TableField("change_log")
    private String changeLog;

    /**
     * 谁改的
     */
    @TableField("changed_by")
    private String changedBy;

    @TableField("changed_at")
    private LocalDateTime changedAt;
}
