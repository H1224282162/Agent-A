package org.example.tool;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

/**
 * 日期时间工具 —— 供 Agent 查询当前时间、计算日期差、获取时区等。
 */
@Component
public class DateTimeTool {

    private static final DateTimeFormatter FULL_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * 获取当前完整日期时间，含时区信息。
     */
    @Tool(description = "获取当前日期和时间（精确到秒），包含时区信息，用于需要知道'现在几点'、'今天几号'等场景")
    public String getCurrentDateTime() {
        ZonedDateTime now = ZonedDateTime.now(ZoneId.of("Asia/Shanghai"));
        return String.format(
                "当前时间: %s\n星期: %s\n时区: %s\nUnix 时间戳: %d 秒",
                now.format(FULL_FMT),
                now.getDayOfWeek(),
                now.getZone(),
                now.toEpochSecond()
        );
    }

    /**
     * 计算两个日期之间相差的天数。
     */
    @Tool(description = "计算两个日期之间相差的天数，参数 startDate 和 endDate 格式为 yyyy-MM-dd，返回相隔天数")
    public String daysBetween(String startDate, String endDate) {
        try {
            LocalDate start = LocalDate.parse(startDate, DATE_FMT);
            LocalDate end = LocalDate.parse(endDate, DATE_FMT);
            long days = ChronoUnit.DAYS.between(start, end);
            return String.format("%s 到 %s 相差 %d 天", startDate, endDate, Math.abs(days));
        } catch (Exception e) {
            return "日期解析失败，请使用 yyyy-MM-dd 格式（如 2026-08-06）。异常: " + e.getMessage();
        }
    }

    /**
     * 获取指定天数之后/之前的日期（正数=未来，负数=过去）。
     */
    @Tool(description = "计算从某个日期开始，经过指定天数后的日期。参数 date: 起始日期(yyyy-MM-dd)；参数 days: 天数，正数向后推，负数向前推")
    public String addDays(String date, int days) {
        try {
            LocalDate start = LocalDate.parse(date, DATE_FMT);
            LocalDate result = start.plusDays(days);
            return String.format("%s %s %d 天 = %s",
                    date,
                    days >= 0 ? "加" : "减",
                    Math.abs(days),
                    result.format(DATE_FMT)
            );
        } catch (Exception e) {
            return "日期解析失败，请使用 yyyy-MM-dd 格式。异常: " + e.getMessage();
        }
    }
}
