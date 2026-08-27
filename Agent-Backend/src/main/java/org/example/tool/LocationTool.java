package org.example.tool;

import org.example.agent.SessionContext;
import org.example.agent.SessionState;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 定位工具 —— Agent 通过此工具获取用户位置并匹配最近的蜜雪冰城门店。
 * <p>
 * 首次调用自动将 {@code hasLocation} 设为 true，
 * 后续调用直接返回已缓存的位置信息。
 */
@Component
public class LocationTool {

    /**
     * 获取当前用户位置及最近门店。
     * <p>
     * 模拟 GPS 定位 —— 首次调用初始化位置，后续返回缓存。
     * Agent 应在用户表达"喝奶茶/蜜雪冰城"意图时第一时间调用此工具。
     */
    @Tool(description = "获取当前用户的地理位置（城市、大致地址）以及最近的一家蜜雪冰城门店信息。首次调用自动定位，后续返回缓存。Agent 应在用户想喝奶茶时先调此工具确认位置。")
    public String getMyLocation() {
        SessionState state = SessionContext.currentState();

        if (state.isHasLocation()) {
            // 已定位，直接返回缓存
            return String.format(
                    "[已定位] 城市: %s | 地址: %s | 最近门店: %s | 距离: 约 350 米",
                    state.getCity(), state.getAddress(), state.getNearbyStore()
            );
        }

        // 首次定位 —— 模拟 GPS + 门店匹配
        String city = "北京";
        String address = "朝阳区望京 SOHO T1 栋";
        String store = "蜜雪冰城(望京 SOHO 店)";

        state.setHasLocation(true);
        state.setCity(city);
        state.setAddress(address);
        state.setNearbyStore(store);

        return String.format(
                "[定位成功] 城市: %s | 地址: %s | 最近门店: %s | 距离: 约 350 米 | 营业中(09:00-22:00)",
                city, address, store
        );
    }
}
