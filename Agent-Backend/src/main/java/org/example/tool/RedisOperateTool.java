package org.example.tool;

import jakarta.annotation.Resource;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Redis 运维工具 —— 供 Agent 通过 function calling 调用，
 * 查询 Redis 服务的运行状态、内存、连接数等监控信息。
 */
@Component
public class RedisOperateTool {

    /**
     * 直接注入连接工厂，执行 INFO 这类服务器级命令比用 RedisTemplate 更自然。
     */
    @Resource
    private RedisConnectionFactory redisConnectionFactory;

    /**
     * 查询 Redis 运行状态。
     * <p>
     * 执行 Redis INFO 命令，提取 Server / Clients / Memory / Stats / CPU /
     * Replication / Keyspace 七个分组中的核心指标，组装成格式化的中文文本返回，
     * 方便 Agent 直接解读并回复用户。
     */
    @Tool(description = "查询Redis当前服务运行状态、内存使用、连接数等监控信息")
    public String getRedisMonitorInfo() {
        try {
            Properties info = redisConnectionFactory.getConnection().info();
            Map<String, String> metrics = extractKeyMetrics(info);
            return formatMetrics(metrics);
        } catch (Exception e) {
            return "[Redis 监控] 无法获取 Redis 信息，请检查 Redis 服务是否正常运行。异常: " + e.getMessage();
        }
    }

    /**
     * 从 INFO 返回的全体属性中挑出用户最关心的指标。
     */
    private Map<String, String> extractKeyMetrics(Properties info) {
        Map<String, String> m = new LinkedHashMap<>();

        // ── Server（服务端信息） ──
        m.put("Redis 版本", info.getProperty("redis_version"));
        m.put("运行模式", info.getProperty("redis_mode"));
        m.put("运行天数", formatUptime(info.getProperty("uptime_in_seconds")));
        m.put("进程 ID", info.getProperty("process_id"));

        // ── Clients（客户端连接） ──
        m.put("当前连接数", info.getProperty("connected_clients"));
        m.put("最大连接数", info.getProperty("maxclients"));
        m.put("阻塞中的客户端", info.getProperty("blocked_clients"));

        // ── Memory（内存） ──
        m.put("当前使用内存", info.getProperty("used_memory_human"));
        m.put("峰值内存", info.getProperty("used_memory_peak_human"));
        m.put("内存碎片率", info.getProperty("mem_fragmentation_ratio"));
        m.put("内存分配器", info.getProperty("mem_allocator"));

        // ── Stats（统计） ──
        m.put("累计接收连接数", info.getProperty("total_connections_received"));
        m.put("累计处理命令数", info.getProperty("total_commands_processed"));
        m.put("键命中次数", info.getProperty("keyspace_hits"));
        m.put("键未命中次数", info.getProperty("keyspace_misses"));
        m.put("缓存命中率", calcHitRate(info));
        m.put("已过期键数", info.getProperty("expired_keys"));
        m.put("因内存淘汰的键数", info.getProperty("evicted_keys"));

        // ── Replication（主从复制） ──
        m.put("节点角色", info.getProperty("role"));
        String slaves = info.getProperty("connected_slaves");
        if (!"0".equals(slaves)) {
            m.put("连接的从节点数", slaves);
        }

        // ── CPU ──
        m.put("CPU 系统态耗时(s)", info.getProperty("used_cpu_sys"));
        m.put("CPU 用户态耗时(s)", info.getProperty("used_cpu_user"));

        // ── Keyspace（数据库键统计） ──
        for (int i = 0; i < 16; i++) {
            String ks = info.getProperty("db" + i);
            if (ks != null && !ks.isEmpty()) {
                m.put("db" + i + " 键统计", ks);
            }
        }

        return m;
    }

    /**
     * 将秒数转为 "X 天 Y 小时 Z 分钟" 的可读格式。
     */
    private String formatUptime(String secondsStr) {
        if (secondsStr == null) return "未知";
        long total = Long.parseLong(secondsStr);
        long days = total / 86400;
        long hours = (total % 86400) / 3600;
        long minutes = (total % 3600) / 60;
        return String.format("%d 天 %d 小时 %d 分钟 (%s 秒)", days, hours, minutes, secondsStr);
    }

    /**
     * 计算缓存命中率百分比。
     */
    private String calcHitRate(Properties info) {
        String hitsStr = info.getProperty("keyspace_hits");
        String missesStr = info.getProperty("keyspace_misses");
        if (hitsStr == null || missesStr == null) return "未知";
        long hits = Long.parseLong(hitsStr);
        long misses = Long.parseLong(missesStr);
        long total = hits + misses;
        if (total == 0) return "0%";
        return String.format("%.2f%%", hits * 100.0 / total);
    }

    /**
     * 将指标 Map 格式化为可读的中文文本，每个指标一行。
     */
    private String formatMetrics(Map<String, String> metrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("===== Redis 服务监控信息 =====\n\n");

        // 按分组输出，每个分组间空一行
        String[] groups = {
                "Redis 版本", "运行模式", "运行天数", "进程 ID",
                "当前连接数", "最大连接数", "阻塞中的客户端",
                "当前使用内存", "峰值内存", "内存碎片率", "内存分配器",
                "累计接收连接数", "累计处理命令数", "键命中次数", "键未命中次数", "缓存命中率", "已过期键数", "因内存淘汰的键数",
                "节点角色", "连接的从节点数",
                "CPU 系统态耗时(s)", "CPU 用户态耗时(s)",
        };

        String currentGroup = null;
        for (String key : groups) {
            // 判断分组切换
            String group = getGroupName(key);
            if (!group.equals(currentGroup)) {
                currentGroup = group;
                sb.append("【").append(group).append("】\n");
            }
            String value = metrics.get(key);
            if (value != null) {
                sb.append("  ").append(key).append(": ").append(value).append("\n");
            }
        }

        // Keyspace 放在最后
        boolean hasKs = metrics.keySet().stream().anyMatch(k -> k.startsWith("db"));
        if (hasKs) {
            sb.append("\n【键空间】\n");
            metrics.forEach((k, v) -> {
                if (k.startsWith("db")) {
                    sb.append("  ").append(k).append(": ").append(v).append("\n");
                }
            });
        }

        return sb.toString();
    }

    /**
     * 将指标 key 映射到对应的分组名。
     */
    private String getGroupName(String key) {
        if (key.startsWith("Redis 版本") || key.startsWith("运行模式")
                || key.startsWith("运行天数") || key.startsWith("进程 ID")) {
            return "服务端";
        }
        if (key.startsWith("当前连接") || key.startsWith("最大连接") || key.startsWith("阻塞")) {
            return "客户端连接";
        }
        if (key.startsWith("当前使用") || key.startsWith("峰值") || key.startsWith("内存碎片") || key.startsWith("内存分配")) {
            return "内存";
        }
        if (key.startsWith("累计") || key.startsWith("键命中") || key.startsWith("键未命中")
                || key.startsWith("缓存命中") || key.startsWith("已过期") || key.startsWith("因内存")) {
            return "统计";
        }
        if (key.startsWith("节点") || key.startsWith("连接")) {
            return "主从复制";
        }
        if (key.startsWith("CPU")) {
            return "CPU";
        }
        return "其他";
    }
}
