package org.example.agent;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 会话上下文 —— 用 ThreadLocal 让工具无需显式传递 sessionId。
 * <p>
 * 每个请求到达时，由拦截器或 Service 层设置当前线程的 sessionId，
 * 工具方法内部通过 {@link #currentSessionId()} 获取，实现"无感透传"。
 * <p>
 * 使用 {@link ConcurrentHashMap} 保存所有会话的状态，模拟真实业务中
 * 的会话数据（登录凭证、用户信息等），实际生产可换成 Redis。
 */
public class SessionContext {

    private static final ThreadLocal<String> CURRENT_SESSION = new ThreadLocal<>();

    /** 全局会话状态存储（模拟 Redis Session） */
    private static final Map<String, SessionState> STATE_MAP = new ConcurrentHashMap<>();

    private SessionContext() {
    }

    // ──────────── ThreadLocal 操作 ────────────

    /** 设置当前线程的 sessionId */
    public static void setCurrentSessionId(String sessionId) {
        CURRENT_SESSION.set(sessionId);
    }

    /** 获取当前线程的 sessionId */
    public static String currentSessionId() {
        String id = CURRENT_SESSION.get();
        if (id == null) {
            throw new IllegalStateException("当前线程未设置 sessionId，请确保请求已通过 SessionContext 初始化");
        }
        return id;
    }

    /** 清除当前线程的 sessionId */
    public static void clear() {
        CURRENT_SESSION.remove();
    }

    // ──────────── 会话状态 ────────────

    /** 获取当前会话的状态（不存在则自动创建） */
    public static SessionState currentState() {
        return STATE_MAP.computeIfAbsent(currentSessionId(), SessionState::new);
    }

    /** 获取指定会话的状态 */
    public static SessionState getState(String sessionId) {
        return STATE_MAP.get(sessionId);
    }

    /** 删除指定会话 */
    public static void removeState(String sessionId) {
        STATE_MAP.remove(sessionId);
    }
}
