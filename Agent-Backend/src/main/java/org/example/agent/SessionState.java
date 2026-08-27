package org.example.agent;

import java.util.ArrayList;
import java.util.List;

/**
 * 会话状态 —— 模拟一个真实用户会话中累积的上下文。
 * <p>
 * 每个会话独立一份，Agent 在做决策时需要参考这些状态来决定下一步行为。
 *
 * <h3>状态流转示意</h3>
 * <pre>
 *   用户说"我想喝蜜雪冰城"
 *     → loggedIn=false, hasLocation=false
 *     → Agent: 先定位 → 再检查登录 → 未登录则让用户登录
 *
 *   用户登录成功
 *     → loggedIn=true, userId="zhangsan", userName="张三"
 *     → Agent: 展示菜单 → 等用户选择
 *
 *   用户说"我要第2个"
 *     → lastRecommendations 中有上次的推荐列表
 *     → Agent: 下单 → 返回订单结果
 * </pre>
 */
public class SessionState {

    // ──────────── 登录状态 ────────────
    private boolean loggedIn;
    private String userId;
    private String userName;

    // ──────────── 位置信息 ────────────
    private boolean hasLocation;
    private String city;
    private String address;
    private String nearbyStore;

    // ──────────── 订单上下文 ────────────
    private String lastRecommendations;     // JSON：上次推荐的饮品列表
    private String selectedDrink;           // 用户选中的饮品名称
    private String selectedDrinkId;         // 用户选中的饮品 ID
    private String lastOrderId;             // 最近一次订单 ID

    // ──────────── 对话轮次 ────────────
    private int turnCount;

    public SessionState(String sessionId) {
        // 初始状态：未登录，无位置
        this.loggedIn = false;
        this.hasLocation = false;
        this.turnCount = 0;
    }

    /** Agent 每调用一次工具或回复用户，轮次 +1 */
    public void incrementTurn() {
        this.turnCount++;
    }

    // ═══════════════════ getter / setter ═══════════════════

    public boolean isLoggedIn() { return loggedIn; }
    public void setLoggedIn(boolean loggedIn) { this.loggedIn = loggedIn; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public boolean isHasLocation() { return hasLocation; }
    public void setHasLocation(boolean hasLocation) { this.hasLocation = hasLocation; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getNearbyStore() { return nearbyStore; }
    public void setNearbyStore(String nearbyStore) { this.nearbyStore = nearbyStore; }

    public String getLastRecommendations() { return lastRecommendations; }
    public void setLastRecommendations(String lastRecommendations) { this.lastRecommendations = lastRecommendations; }

    public String getSelectedDrink() { return selectedDrink; }
    public void setSelectedDrink(String selectedDrink) { this.selectedDrink = selectedDrink; }

    public String getSelectedDrinkId() { return selectedDrinkId; }
    public void setSelectedDrinkId(String selectedDrinkId) { this.selectedDrinkId = selectedDrinkId; }

    public String getLastOrderId() { return lastOrderId; }
    public void setLastOrderId(String lastOrderId) { this.lastOrderId = lastOrderId; }

    public int getTurnCount() { return turnCount; }
}
