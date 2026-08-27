package org.example.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.example.model.ToolDef;
import org.example.service.IToolDefService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Map;

/**
 * 工具扫描器 —— 应用启动完成后，扫描所有带 {@link Tool @Tool} 注解的方法，
 * 将工具元数据（名称、描述、参数定义等）自动同步到 {@code tool_def} 表。
 *
 * <h3>同步策略</h3>
 * <ul>
 *   <li>新工具（tool_name 不在表中）→ INSERT</li>
 *   <li>已有工具（tool_name 已存在）→ 更新描述和参数定义</li>
 *   <li>代码中已删除的工具 → 标记为禁用（status=0），不删除记录</li>
 * </ul>
 */
@Component
public class ToolScanner {

    private static final Logger log = LoggerFactory.getLogger(ToolScanner.class);

    private final ApplicationContext applicationContext;
    private final IToolDefService toolDefService;
    private final ObjectMapper objectMapper;

    public ToolScanner(ApplicationContext applicationContext,
                       IToolDefService toolDefService,
                       ObjectMapper objectMapper) {
        this.applicationContext = applicationContext;
        this.toolDefService = toolDefService;
        this.objectMapper = objectMapper;
    }

    /**
     * 应用就绪后立即执行一次全量扫描。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void scanAndSync() {
        log.info("===== 开始扫描 @Tool 注解，同步到 tool_def 表 =====");

        // 1. 查出数据库中现存的所有工具
        List<ToolDef> existingTools = toolDefService.list();
        Map<String, ToolDef> existingMap = new java.util.LinkedHashMap<>();
        for (ToolDef t : existingTools) {
            existingMap.put(t.getToolName(), t);
        }

        // 2. 扫描 Spring 容器中所有带 @Tool 方法的 Bean
        Map<String, ToolMeta> scannedTools = new java.util.LinkedHashMap<>();
        String[] beanNames = applicationContext.getBeanDefinitionNames();

        for (String beanName : beanNames) {
            Object bean;
            try {
                bean = applicationContext.getBean(beanName);
            } catch (Exception e) {
                continue; // 跳过无法获取的 Bean
            }

            Class<?> clazz = bean.getClass();
            // 处理 CGLIB 代理，拿原始类
            if (clazz.getName().contains("$$")) {
                clazz = clazz.getSuperclass();
            }

            for (Method method : clazz.getDeclaredMethods()) {
                Tool toolAnno = method.getAnnotation(Tool.class);
                if (toolAnno == null) continue;

                // 用类名首字母小写作为 bean name（Spring 默认规则）
                String simpleClassName = clazz.getSimpleName();
                String toolBeanName = Character.toLowerCase(simpleClassName.charAt(0))
                        + simpleClassName.substring(1);

                ToolMeta meta = new ToolMeta();
                meta.beanName = toolBeanName;
                meta.methodName = method.getName();
                meta.description = toolAnno.description();
                meta.parameters = extractParameters(method);
                meta.category = guessCategory(simpleClassName);

                // key = "方法名" 作为 tool_name，与数据库匹配
                scannedTools.put(method.getName(), meta);
            }
        }

        // 3. 同步：新增或更新
        int inserted = 0, updated = 0;
        for (Map.Entry<String, ToolMeta> entry : scannedTools.entrySet()) {
            String toolName = entry.getKey();
            ToolMeta meta = entry.getValue();

            if (existingMap.containsKey(toolName)) {
                // 已存在 → 更新描述和参数
                ToolDef existing = existingMap.get(toolName);
                boolean changed = false;

                if (!meta.description.equals(existing.getDescription())) {
                    existing.setDescription(meta.description);
                    changed = true;
                }
                if (!meta.parameters.equals(existing.getParameters())) {
                    existing.setParameters(meta.parameters);
                    changed = true;
                }
                if (existing.getStatus() != 1) {
                    existing.setStatus((byte) 1); // 重新启用
                    changed = true;
                }

                if (changed) {
                    toolDefService.updateById(existing);
                    updated++;
                    log.info("  [更新] {} — 描述/参数已变化", toolName);
                }
                existingMap.remove(toolName); // 标记为"已处理"
            } else {
                // 新工具 → 插入
                ToolDef def = new ToolDef();
                def.setToolName(toolName);
                def.setDisplayName(extractDisplayName(meta));
                def.setCategory(meta.category);
                def.setDescription(meta.description);
                def.setParameters(meta.parameters);
                def.setStatus((byte) 1);
                toolDefService.save(def);
                inserted++;
                log.info("  [新增] {} — 已同步到 tool_def 表", toolName);
            }
        }

        // 4. 剩余在 existingMap 中的是代码中已删除的工具 → 禁用
        int disabled = 0;
        for (ToolDef leftover : existingMap.values()) {
            if (leftover.getStatus() == 1) {
                leftover.setStatus((byte) 0);
                toolDefService.updateById(leftover);
                disabled++;
                log.info("  [禁用] {} — 代码中已无此工具", leftover.getToolName());
            }
        }

        log.info("===== 扫描完成：新增 {} | 更新 {} | 禁用 {} =====", inserted, updated, disabled);
    }

    /**
     * 提取方法的参数定义，序列化为 JSON 数组。
     * 格式: [{"name":"account","type":"String"}, ...]
     */
    private String extractParameters(Method method) {
        ArrayNode arr = objectMapper.createArrayNode();
        for (Parameter p : method.getParameters()) {
            ObjectNode node = objectMapper.createObjectNode();
            node.put("name", p.getName());
            node.put("type", p.getType().getSimpleName());
            arr.add(node);
        }
        return arr.toString();
    }

    /**
     * 根据类名猜测工具分类。
     */
    private String guessCategory(String className) {
        if (className.contains("Location") || className.contains("Auth")
                || className.contains("Menu") || className.contains("Order")) {
            return "业务工具";
        }
        if (className.contains("Redis")) {
            return "运维工具";
        }
        if (className.contains("System") || className.contains("DateTime")) {
            return "系统工具";
        }
        return "其他";
    }

    /**
     * 为工具生成一个可读的中文显示名。
     */
    private String extractDisplayName(ToolMeta meta) {
        // 取类名去掉 "Tool" 后缀
        String className = meta.beanName;
        if (className.endsWith("Tool")) {
            className = className.substring(0, className.length() - 4);
        }
        // 首字母大写
        return Character.toUpperCase(className.charAt(0)) + className.substring(1) + "工具";
    }

    /** 工具元数据内部类 */
    private static class ToolMeta {
        String beanName;
        String methodName;
        String description;
        String parameters;
        String category;
    }
}
