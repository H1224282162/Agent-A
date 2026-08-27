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
@TableName("agent_call_log")
public class AgentCallLog implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @TableField("agent_id")
    private Long agentId;

    @TableField("session_id")
    private String sessionId;

    @TableField("user_input")
    private String userInput;

    @TableField("agent_output")
    private String agentOutput;

    /**
     * 调了哪些工具、耗时
     */
    @TableField("tool_calls")
    private String toolCalls;

    /**
     * 总耗时
     */
    @TableField("latency_ms")
    private Integer latencyMs;

    /**
     * success / error
     */
    @TableField("status")
    private String status;

    @TableField("created_at")
    private LocalDateTime createdAt;
}
