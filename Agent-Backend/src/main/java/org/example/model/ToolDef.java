package org.example.model;

import com.baomidou.mybatisplus.annotation.IdType;
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
@TableName("tool_def")
public class ToolDef implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 对应 @Component 的 bean name
     */
    @TableField("tool_name")
    private String toolName;

    /**
     * 管理后台显示名："定位工具"
     */
    @TableField("display_name")
    private String displayName;

    /**
     * 分类：业务/运维/代码
     */
    @TableField("category")
    private String category;

    /**
     * 工具说明
     */
    @TableField("description")
    private String description;

    /**
     * 参数定义（从 @Tool 注解自动采集）
     */
    @TableField("parameters")
    private String parameters;

    /**
     * 1启用 0禁用
     */
    @TableField("status")
    private Byte status;
}
