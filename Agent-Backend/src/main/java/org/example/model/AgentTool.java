package org.example.model;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

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
@TableName("agent_tool")
public class AgentTool implements Serializable {
    private static final long serialVersionUID = 1L;

    private Long agentId;

    private Long toolId;

    /**
     * 可临时禁用某个工具的绑定而不删记录
     */
    @TableField("enabled")
    private Byte enabled;
}
