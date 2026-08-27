package org.example.tool;

import org.example.agent.SessionContext;
import org.example.agent.SessionState;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 菜单推荐工具 —— Agent 根据用户所在城市/天气推荐合适的饮品。
 * <p>
 * 推荐结果会写入 SessionState.lastRecommendations，供后续"选第几个"时回溯。
 */
@Component
public class MenuTool {

    private static final Map<String, MenuItem> MENU = new LinkedHashMap<>();

    static {
        MENU.put("drink_01", new MenuItem("drink_01", "冰鲜柠檬水",   4, "经典款，清爽解暑"));
        MENU.put("drink_02", new MenuItem("drink_02", "珍珠奶茶",     7, "Q弹珍珠配浓郁奶茶"));
        MENU.put("drink_03", new MenuItem("drink_03", "杨枝甘露",     9, "芒果+西柚+椰奶，口感丰富"));
        MENU.put("drink_04", new MenuItem("drink_04", "满杯百香果",   8, "酸甜百香果，满满维C"));
        MENU.put("drink_05", new MenuItem("drink_05", "椰椰芒芒",    10, "椰奶+芒果，热带风情"));
        MENU.put("drink_06", new MenuItem("drink_06", "蜜桃乌龙茶",   6, "蜜桃清香配乌龙回甘"));
        MENU.put("drink_07", new MenuItem("drink_07", "草莓奶昔",    12, "新鲜草莓+冰淇淋"));
        MENU.put("drink_08", new MenuItem("drink_08", "柠檬红茶",     5, "柠香与红茶的完美融合"));
        MENU.put("drink_09", new MenuItem("drink_09", "烧仙草奶茶",   8, "仙草+红豆+花生，料超足"));
        MENU.put("drink_10", new MenuItem("drink_10", "冰淇淋咖啡",  10, "阿芙佳朵风格，冰火两重天"));
    }

    /**
     * 根据价格区间过滤并推荐饮品。
     * <p>
     * 成功后写入 {@code lastRecommendations}（JSON），
     * 供 Agent 后续识别"第2个"、"刚才推荐的柠檬水"等指代。
     *
     * @param minPrice 最低价格（含），传 0 表示不限
     * @param maxPrice 最高价格（含），传 0 表示不限
     */
    @Tool(description = "获取蜜雪冰城推荐饮品列表。参数 minPrice: 最低价格；参数 maxPrice: 最高价格（0 表示不限制）。Agent 应在用户已登录且有位置信息后调用。")
    public String getRecommendedDrinks(int minPrice, int maxPrice) {
        SessionState state = SessionContext.currentState();

        // 前置检查
        if (!state.isLoggedIn()) {
            return "[操作被拒] 需要先登录才能查看菜单，请先引导用户登录。";
        }
        if (!state.isHasLocation()) {
            return "[操作被拒] 需要先获取位置信息才能推荐附近门店的饮品，请先调用 getMyLocation。";
        }

        // 构建推荐列表
        StringBuilder sb = new StringBuilder();
        sb.append("===== 蜜雪冰城推荐饮品 =====\n");
        sb.append("门店: ").append(state.getNearbyStore()).append("\n");
        sb.append(String.format("价格区间: %s ~ %s\n\n",
                minPrice <= 0 ? "不限" : "¥" + minPrice,
                maxPrice <= 0 ? "不限" : "¥" + maxPrice));

        StringBuilder jsonCache = new StringBuilder("[");
        int index = 0;
        int count = 0;

        for (MenuItem item : MENU.values()) {
            if (minPrice > 0 && item.price < minPrice) continue;
            if (maxPrice > 0 && item.price > maxPrice) continue;

            count++;
            index++;
            sb.append(String.format("  %d. %s  ¥%d — %s\n", index, item.name, item.price, item.desc));

            if (jsonCache.length() > 1) jsonCache.append(",");
            jsonCache.append(String.format("{\"index\":%d,\"id\":\"%s\",\"name\":\"%s\",\"price\":%d}",
                    index, item.id, item.name, item.price));
        }
        jsonCache.append("]");

        // 写入推荐缓存，支持后续"第N个"的指代消解
        state.setLastRecommendations(jsonCache.toString());

        if (count == 0) {
            sb.append("  (该价格区间暂无饮品，请扩大范围)");
        } else {
            sb.append(String.format("\n共 %d 款饮品。请告诉用户编号即可下单。", count));
        }

        return sb.toString();
    }

    // 内部类：菜单项
    private record MenuItem(String id, String name, int price, String desc) {}
}
