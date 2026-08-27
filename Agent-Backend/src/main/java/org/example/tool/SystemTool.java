package org.example.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.time.Duration;

/**
 * 系统信息工具 —— 供 Agent 查询 JVM 内存使用、CPU 核数、系统负载等运行时信息。
 */
@Component
public class SystemTool {

    /**
     * 获取 JVM 运行时状态：内存使用、CPU 核数、进程运行时长。
     */
    @Tool(description = "查询当前 Java 服务的 JVM 内存使用情况、CPU 核数、进程运行时长等运行时信息，用于诊断服务是否正常、是否内存不足")
    public String getJvmInfo() {
        MemoryMXBean memory = ManagementFactory.getMemoryMXBean();
        OperatingSystemMXBean os = ManagementFactory.getOperatingSystemMXBean();
        RuntimeMXBean runtime = ManagementFactory.getRuntimeMXBean();

        // 内存信息
        long heapUsed = memory.getHeapMemoryUsage().getUsed();
        long heapMax = memory.getHeapMemoryUsage().getMax();
        long nonHeapUsed = memory.getNonHeapMemoryUsage().getUsed();

        // 进程运行时长
        long uptimeMs = runtime.getUptime();
        Duration uptime = Duration.ofMillis(uptimeMs);
        long hours = uptime.toHours();
        long minutes = uptime.toMinutesPart();
        long seconds = uptime.toSecondsPart();

        // CPU 核数 + 系统平均负载（仅 Unix/Linux/macOS 有效）
        int processors = os.getAvailableProcessors();
        String loadAvg = "不可用（当前平台不支持）";
        try {
            double load = os.getSystemLoadAverage();
            if (load >= 0) {
                loadAvg = String.format("%.2f", load);
            }
        } catch (Exception ignored) {
            // Windows 上 getSystemLoadAverage 返回 -1
        }

        return String.format(
                "===== JVM 运行时信息 =====\n\n"
                        + "【内存】\n"
                        + "  堆内存已用: %s\n"
                        + "  堆内存上限: %s\n"
                        + "  非堆内存已用: %s\n\n"
                        + "【CPU】\n"
                        + "  可用处理器数: %d 核\n"
                        + "  系统负载均值: %s\n\n"
                        + "【进程】\n"
                        + "  进程已运行: %d 小时 %d 分 %d 秒\n"
                        + "  PID: %s\n",
                formatBytes(heapUsed),
                formatBytes(heapMax),
                formatBytes(nonHeapUsed),
                processors,
                loadAvg,
                hours, minutes, seconds,
                runtime.getPid()
        );
    }

    /**
     * 获取系统可用处理器数量。
     */
    @Tool(description = "查询当前服务器的 CPU 逻辑核心数")
    public int getCpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * 字节转为可读字符串。
     */
    private String formatBytes(long bytes) {
        if (bytes < 0) return "未知";
        if (bytes < 1024) return bytes + " B";
        long kb = bytes / 1024;
        if (kb < 1024) return kb + " KB";
        long mb = kb / 1024;
        if (mb < 1024) return mb + " MB";
        return String.format("%.2f GB", mb / 1024.0);
    }
}
