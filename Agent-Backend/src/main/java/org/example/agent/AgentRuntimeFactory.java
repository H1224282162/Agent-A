package org.example.agent;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.example.config.model.ModelRegistry;
import org.example.model.AgentDef;
import org.example.model.AgentTool;
import org.example.model.ToolDef;
import org.example.knowledge.rag.RagAdvisor;
import org.example.service.IAgentDefService;
import org.example.service.IAgentToolService;
import org.example.service.IToolDefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Agent 运行时工厂 —— 核心组装器。
 *
 * <h3>职责</h3>
 * <ol>
 *   <li>启动时从数据库加载所有启用（status=1）的 Agent 定义</li>
 *   <li>根据 agent_tool 关联表找到 Agent 绑定的工具</li>
 *   <li>从 Spring 容器中取出对应的工具 Bean</li>
 *   <li>组装 ChatClient（System Prompt + Tools + MessageChatMemoryAdvisor）</li>
 *   <li>封装为 {@link AgentRuntime} 并缓存到内存</li>
 * </ol>
 *
 * <h3>热加载</h3>
 * 调用 {@link #reload(String)} 可重新从数据库加载指定 Agent，
 * 后台修改 Prompt 或工具绑定后无需重启应用。
 */
@Component
public class AgentRuntimeFactory {

    private static final Logger log = LoggerFactory.getLogger(AgentRuntimeFactory.class);

    /** Agent 运行时缓存：agentCode → AgentRuntime */
    private final Map<String, AgentRuntime> cache = new ConcurrentHashMap<>();

    private final ApplicationContext applicationContext;
    private final IAgentDefService agentDefService;
    private final IAgentToolService agentToolService;
    private final IToolDefService toolDefService;
    private final ModelRegistry modelRegistry;
    private final ChatMemory chatMemory;
    private final RagAdvisor ragAdvisor;

    public AgentRuntimeFactory(
            ApplicationContext applicationContext,
            IAgentDefService agentDefService,
            IAgentToolService agentToolService,
            IToolDefService toolDefService,
            ModelRegistry modelRegistry,
            ChatMemory chatMemory,
            RagAdvisor ragAdvisor
    ) {
        this.applicationContext = applicationContext;
        this.agentDefService = agentDefService;
        this.agentToolService = agentToolService;
        this.toolDefService = toolDefService;
        this.modelRegistry = modelRegistry;
        this.chatMemory = chatMemory;
        this.ragAdvisor = ragAdvisor;
    }

    /**
     * 应用就绪后，从数据库加载所有启用的 Agent。
     * 执行时机在 ToolScanner 之后，确保 tool_def 已是最新。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("===== 开始从数据库加载 Agent 定义 =====");

        List<AgentDef> enabledAgents = agentDefService.list(
                new LambdaQueryWrapper<AgentDef>().eq(AgentDef::getStatus, (byte) 1)
        );

        int loaded = 0;
        for (AgentDef def : enabledAgents) {
            try {
                AgentRuntime runtime = assemble(def);
                cache.put(def.getAgentCode(), runtime);
                loaded++;
                log.info("  [加载] {} — 工具: {}", def.getAgentCode(), runtime.toolNames());
            } catch (Exception e) {
                log.error("  [失败] {} — 组装异常: {}", def.getAgentCode(), e.getMessage(), e);
            }
        }

        log.info("===== 加载完成：{}/{} 个 Agent 就绪 =====", loaded, enabledAgents.size());
    }

    /**
     * 根据 agentCode 获取已缓存的 AgentRuntime。
     *
     * @throws IllegalArgumentException 如果 Agent 不存在
     */
    public AgentRuntime get(String agentCode) {
        AgentRuntime rt = cache.get(agentCode);
        if (rt == null) {
            throw new IllegalArgumentException("Agent 不存在或未启用: " + agentCode);
        }
        return rt;
    }

    /**
     * 根据 agentCode 获取 Agent，并允许覆盖默认模型类型。
     * <p>
     * 当 {@code modelType} 为空或与 Agent 默认模型一致时，直接返回缓存实例；
     * 否则从数据库重新加载定义、按指定模型重新组装（不写回缓存，避免污染默认配置）。
     *
     * @param agentCode Agent 编码
     * @param modelType 指定模型类型（对应 ModelRegistry 中的 name，如 deepseek / kimi）
     * @throws IllegalArgumentException 如果 Agent 不存在或未配置该模型
     */
    public AgentRuntime get(String agentCode, String modelType) {
        AgentRuntime cached = get(agentCode);
        if (modelType == null || modelType.isBlank() || modelType.trim().equals(cached.modelType())) {
            return cached;
        }

        // 指定了不同模型：重新加载定义并按指定模型组装
        AgentDef def = agentDefService.getOne(
                new LambdaQueryWrapper<AgentDef>()
                        .eq(AgentDef::getAgentCode, agentCode)
                        .eq(AgentDef::getStatus, (byte) 1)
        );
        if (def == null) {
            throw new IllegalArgumentException("Agent 不存在或未启用: " + agentCode);
        }
        return assemble(def, modelType.trim());
    }

    /**
     * 获取所有已加载的 Agent 编码列表。
     */
    public List<String> listAgentCodes() {
        return new ArrayList<>(cache.keySet());
    }

    /**
     * 热加载：从数据库重新加载指定 Agent，替换缓存中的旧版本。
     */
    public AgentRuntime reload(String agentCode) {
        AgentDef def = agentDefService.getOne(
                new LambdaQueryWrapper<AgentDef>()
                        .eq(AgentDef::getAgentCode, agentCode)
                        .eq(AgentDef::getStatus, (byte) 1)
        );
        if (def == null) {
            log.warn("热加载失败：Agent {} 不存在或已禁用", agentCode);
            return null;
        }

        AgentRuntime runtime = assemble(def);
        cache.put(agentCode, runtime);
        log.info("热加载完成：{} v{}，工具: {}", agentCode, def.getVersion(), runtime.toolNames());
        return runtime;
    }

    /**
     * 组装一个 AgentRuntime：从 DB 定义 → 找到工具 Bean → 组装 ChatClient。
     */
    private AgentRuntime assemble(AgentDef def) {
        return assemble(def, def.getModelType() != null ? def.getModelType() : "deepseek");
    }

    /**
     * 按指定模型类型组装 AgentRuntime（{@code modelType} 覆盖 Agent 默认模型）。
     */
    private AgentRuntime assemble(AgentDef def, String modelType) {
        // 1. 构建 AgentDefinition
        List<String> toolNames = getToolNamesForAgent(def.getId());
        AgentDefinition definition = new AgentDefinition(
                def.getAgentCode(),
                def.getAgentName(),
                def.getSystemPrompt(),
                modelType,
                def.getVersion() != null ? def.getVersion() : 1,
                toolNames
        );

        // 2. 从 Spring 容器中找到对应的工具 Bean
        Object[] toolBeans = resolveToolBeans(toolNames);

        // 3. 选择 ChatModel（通过注册表按 modelType 路由）
        ChatModel chatModel = modelRegistry.getChatModel(definition.modelType());

        // 4. 组装
        ChatClient chatClient = ChatClient.builder(chatModel)
                .defaultSystem(definition.systemPrompt())
                .defaultAdvisors(
                        MessageChatMemoryAdvisor.builder(chatMemory).build(),
                        ragAdvisor
                )
                .defaultTools(toolBeans)
                .build();

        return new AgentRuntime(definition, chatClient);
    }

    /**
     * 查询 agent_tool 关联表，获取 Agent 绑定的工具方法名列表。
     */
    private List<String> getToolNamesForAgent(Long agentId) {
        List<AgentTool> bindings = agentToolService.list(
                new LambdaQueryWrapper<AgentTool>()
                        .eq(AgentTool::getAgentId, agentId)
                        .eq(AgentTool::getEnabled, (byte) 1)
        );

        if (bindings.isEmpty()) return List.of();

        // tool_id → tool_name
        List<Long> toolIds = bindings.stream().map(AgentTool::getToolId).toList();
        List<ToolDef> toolDefs = toolDefService.list(
                new LambdaQueryWrapper<ToolDef>()
                        .in(ToolDef::getId, toolIds)
                        .eq(ToolDef::getStatus, (byte) 1)
        );

        return toolDefs.stream().map(ToolDef::getToolName).toList();
    }

    /**
     * 根据工具方法名列表，从 Spring 容器中取出对应的 Bean。
     * <p>
     * 每个 @Tool 注解方法的 tool_name 就是方法名，
     * 需要根据方法名反查所属的 Bean。
     */
    private Object[] resolveToolBeans(List<String> toolNames) {
        if (toolNames.isEmpty()) return new Object[0];

        Set<Object> beans = new HashSet<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String toolName : toolNames) {
            Object found = null;
            for (String beanName : beanNames) {
                Object bean;
                try {
                    bean = applicationContext.getBean(beanName);
                } catch (Exception e) {
                    continue;
                }
                Class<?> clazz = bean.getClass();
                if (clazz.getName().contains("$$")) {
                    clazz = clazz.getSuperclass();
                }
                // 遍历方法，看哪个方法名 == toolName
                for (java.lang.reflect.Method m : clazz.getDeclaredMethods()) {
                    if (m.getName().equals(toolName)
                            && m.getAnnotation(org.springframework.ai.tool.annotation.Tool.class) != null) {
                        found = bean;
                        break;
                    }
                }
                if (found != null) break;
            }
            if (found != null) {
                beans.add(found);
            } else {
                log.warn("工具 Bean 未找到: {}，将被跳过", toolName);
            }
        }

        return beans.toArray();
    }
}
