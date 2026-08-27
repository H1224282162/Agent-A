package org.example.model;

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
 * Agent-知识库绑定关系表
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Getter
@Setter
@ToString
@TableName("agent_knowledge")
public class AgentKnowledge implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * Agent ID
     */
    @TableId("agent_id")
    private Long agentId;

    /**
     * 知识库 ID
     */
    @TableField("kb_id")
    private Long kbId;

    /**
     * 是否启用该知识库绑定：1启用 0禁用
     */
    @TableField("enabled")
    private Byte enabled;

    /**
     * 创建时间
     */
    @TableField("created_at")
    private LocalDateTime createdAt;
}
