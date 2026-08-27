package org.example.service;

import com.baomidou.mybatisplus.spring.service.IService;
import org.example.model.AgentKnowledge;

import java.util.List;

/**
 * <p>
 * Agent-知识库绑定关系表 服务类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
public interface IAgentKnowledgeService extends IService<AgentKnowledge> {

    /**
     * 查询 Agent 已启用的知识库绑定
     *
     * @param agentId Agent ID
     * @return 绑定列表
     */
    List<AgentKnowledge> listEnabledByAgentId(Long agentId);

}
