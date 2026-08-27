package org.example.service.impl;

import org.example.model.AgentCallLog;
import org.example.mapper.AgentCallLogMapper;
import org.example.service.IAgentCallLogService;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author wzh
 * @since 2026-08-08
 */
@Service
public class AgentCallLogServiceImpl extends ServiceImpl<AgentCallLogMapper, AgentCallLog> implements IAgentCallLogService {

}
