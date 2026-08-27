package org.example.tool;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.agent.SessionContext;
import org.example.agent.SessionState;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 下单工具 —— Agent 在用户确认饮品后，调用此工具完成模拟下单。
 */
@Component
public class OrderTool {

    private final ObjectMapper objectMapper;

    public OrderTool(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    /**
     * 下单：根据用户在推荐列表中选择的序号，查找对应饮品并完成下单。
     *
     * @param choice 用户选择：可以是序号（如 "1", "2"），也可以是饮品名称（如 "冰鲜柠檬水"）
     */
    @Tool(description = "为用户下单指定的饮品。参数 choice: 用户在推荐列表中选择的序号（如'2'）或饮品名称（如'珍珠奶茶'）。Agent 应在用户明确选择后调用。")
    public String placeOrder(String choice) {
        SessionState state = SessionContext.currentState();

        // ── 前置检查 ──
        if (!state.isLoggedIn()) {
            return "[下单失败] 尚未登录，请先引导用户登录后再下单。";
        }
        if (!state.isHasLocation()) {
            return "[下单失败] 尚未获取位置信息，请先调用 getMyLocation。";
        }
        if (state.getLastRecommendations() == null) {
            return "[下单失败] 尚未获取饮品菜单，请先调用 getRecommendedDrinks 展示菜单。";
        }

        // ── 解析用户选择 ──
        try {
            List<Map<String, Object>> items = objectMapper.readValue(
                    state.getLastRecommendations(),
                    new TypeReference<>() {}
            );

            Map<String, Object> selected = findItem(items, choice.trim());
            if (selected == null) {
                StringBuilder hint = new StringBuilder();
                hint.append("[选择无效] 没找到匹配的饮品。可选编号: ");
                for (Map<String, Object> item : items) {
                    hint.append(item.get("index")).append("(").append(item.get("name")).append(")、");
                }
                hint.deleteCharAt(hint.length() - 1);
                hint.append("。请让用户重新选择。");
                return hint.toString();
            }

            // ── 生成订单 ──
            String orderId = "MXBC" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            String drinkName = (String) selected.get("name");
            int price = (int) selected.get("price");

            state.setSelectedDrinkId((String) selected.get("id"));
            state.setSelectedDrink(drinkName);
            state.setLastOrderId(orderId);

            return String.format(
                    "===== 下单成功！=====\n"
                            + "订单编号: %s\n"
                            + "商品: %s  ×1\n"
                            + "金额: ¥%d\n"
                            + "取餐门店: %s\n"
                            + "预计取餐时间: 15-20 分钟\n"
                            + "用户: %s\n\n"
                            + "请告知用户订单已生成，取餐时凭订单编号即可。",
                    orderId,
                    drinkName,
                    price,
                    state.getNearbyStore(),
                    state.getUserName()
            );

        } catch (Exception e) {
            return "[下单异常] 解析推荐列表失败: " + e.getMessage();
        }
    }

    /**
     * 查询最近一笔订单的状态。
     */
    @Tool(description = "查询用户最近一笔订单的详细信息（订单编号、商品、金额、取餐门店等）")
    public String getLastOrder() {
        SessionState state = SessionContext.currentState();

        if (state.getLastOrderId() == null) {
            return "[查询失败] 当前会话还没有下过单。";
        }

        return String.format(
                "===== 最近订单 =====\n"
                        + "订单编号: %s\n"
                        + "商品: %s\n"
                        + "取餐门店: %s\n"
                        + "用户: %s\n"
                        + "状态: 制作中（预计 10 分钟后可取餐）",
                state.getLastOrderId(),
                state.getSelectedDrink(),
                state.getNearbyStore(),
                state.getUserName()
        );
    }

    /** 根据序号或名称从推荐列表中查找饮品 */
    private Map<String, Object> findItem(List<Map<String, Object>> items, String choice) {
        // 先按序号匹配
        try {
            int idx = Integer.parseInt(choice);
            for (Map<String, Object> item : items) {
                if (item.get("index") instanceof Integer i && i == idx) {
                    return item;
                }
            }
        } catch (NumberFormatException ignored) {
            // 非数字，按名称模糊匹配
        }

        // 按名称模糊匹配
        for (Map<String, Object> item : items) {
            String name = (String) item.get("name");
            if (name != null && (name.equals(choice) || name.contains(choice))) {
                return item;
            }
        }
        return null;
    }
}
