package org.example.Controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.common.Result;
import org.example.model.AgentKnowledge;
import org.example.model.KnowledgeBase;
import org.example.service.IAgentKnowledgeService;
import org.example.service.IKnowledgeBaseService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * <p>
 * Agent-知识库绑定关系表 前端控制器
 * </p>
 *
 * @author wzh
 * @since 2026-08-18
 */
@RestController
@RequestMapping("/agentKnowledge")
public class AgentKnowledgeController {

    private final IAgentKnowledgeService agentKnowledgeService;
    private final IKnowledgeBaseService knowledgeBaseService;

    public AgentKnowledgeController(IAgentKnowledgeService agentKnowledgeService,
                                    IKnowledgeBaseService knowledgeBaseService) {
        this.agentKnowledgeService = agentKnowledgeService;
        this.knowledgeBaseService = knowledgeBaseService;
    }

    /**
     * 查询 Agent 已绑定的知识库
     */
    @GetMapping("/list")
    public Result<List<Map<String, Object>>> list(@RequestParam Long agentId) {
        LambdaQueryWrapper<AgentKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledge::getAgentId, agentId);
        List<AgentKnowledge> bindings = agentKnowledgeService.list(wrapper);

        if (bindings.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        List<Long> kbIds = bindings.stream().map(AgentKnowledge::getKbId).toList();
        Map<Long, KnowledgeBase> kbMap = knowledgeBaseService.listByIds(kbIds).stream()
                .collect(Collectors.toMap(KnowledgeBase::getId, kb -> kb));

        List<Map<String, Object>> result = new ArrayList<>();
        for (AgentKnowledge binding : bindings) {
            Map<String, Object> item = new HashMap<>();
            item.put("agentId", binding.getAgentId());
            item.put("kbId", binding.getKbId());
            item.put("enabled", binding.getEnabled());
            item.put("createdAt", binding.getCreatedAt());

            KnowledgeBase kb = kbMap.get(binding.getKbId());
            if (kb != null) {
                item.put("kbCode", kb.getKbCode());
                item.put("kbName", kb.getKbName());
            }
            result.add(item);
        }
        return Result.success(result);
    }

    /**
     * 绑定知识库
     */
    @PostMapping("/bind")
    public Result<Void> bind(@RequestBody AgentKnowledge binding) {
        if (binding.getAgentId() == null || binding.getKbId() == null) {
            return Result.fail("agentId 和 kbId 不能为空");
        }

        LambdaQueryWrapper<AgentKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledge::getAgentId, binding.getAgentId());
        wrapper.eq(AgentKnowledge::getKbId, binding.getKbId());
        AgentKnowledge existing = agentKnowledgeService.getOne(wrapper);

        if (existing != null) {
            existing.setEnabled((byte) 1);
            agentKnowledgeService.updateById(existing);
        } else {
            binding.setEnabled((byte) 1);
            binding.setCreatedAt(LocalDateTime.now());
            agentKnowledgeService.save(binding);
        }
        return Result.success("绑定成功", null);
    }

    /**
     * 解绑知识库
     */
    @DeleteMapping("/unbind")
    public Result<Void> unbind(@RequestParam Long agentId, @RequestParam Long kbId) {
        LambdaQueryWrapper<AgentKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledge::getAgentId, agentId);
        wrapper.eq(AgentKnowledge::getKbId, kbId);
        agentKnowledgeService.remove(wrapper);
        return Result.success("解绑成功", null);
    }

    /**
     * 切换绑定启用/禁用状态
     */
    @PutMapping("/toggle")
    public Result<Void> toggle(@RequestParam Long agentId, @RequestParam Long kbId) {
        LambdaQueryWrapper<AgentKnowledge> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(AgentKnowledge::getAgentId, agentId);
        wrapper.eq(AgentKnowledge::getKbId, kbId);
        AgentKnowledge existing = agentKnowledgeService.getOne(wrapper);

        if (existing == null) {
            return Result.fail("绑定关系不存在");
        }
        existing.setEnabled(existing.getEnabled() != null && existing.getEnabled() == 1 ? (byte) 0 : (byte) 1);
        agentKnowledgeService.updateById(existing);
        return Result.success("状态切换成功", null);
    }

}
