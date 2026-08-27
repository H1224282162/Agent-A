package org.example.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.example.model.AgentKnowledge;
import org.example.mapper.AgentKnowledgeMapper;
import org.example.service.IAgentKnowledgeService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * <p>
 * Agent-知识库绑定关系表 服务实现类
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@Service
public class AgentKnowledgeServiceImpl extends ServiceImpl<AgentKnowledgeMapper, AgentKnowledge> implements IAgentKnowledgeService {

    @Override
    public List<AgentKnowledge> listEnabledByAgentId(Long agentId) {
        LambdaQueryWrapper<AgentKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledge::getAgentId, agentId);
        wrapper.eq(AgentKnowledge::getEnabled, (byte) 1);
        return list(wrapper);
    }

}
