package org.example.tool;

import org.example.agent.SessionContext;
import org.example.agent.SessionState;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * 登录鉴权工具 —— Agent 通过此工具检查登录状态、模拟登录。
 * <p>
 * 模拟用户系统：内置两个测试账号，登录成功后将用户信息写入 SessionState。
 */
@Component
public class AuthTool {

    // 模拟用户数据库
    private static final String MOCK_USER_ACCOUNT = "zhangsan";
    private static final String MOCK_USER_PASSWORD = "123456";
    private static final String MOCK_USER_NAME = "张三";

    /**
     * 检查当前会话的登录状态。
     * <p>
     * Agent 应在进入"点单"流程前调此工具确认用户是否已登录。
     */
    @Tool(description = "检查当前用户的登录状态。返回是否已登录、用户 ID、用户名等信息。Agent 在用户要求下单前必须调用此工具，若未登录则引导用户输入账号密码。")
    public String checkLoginStatus() {
        SessionState state = SessionContext.currentState();

        if (state.isLoggedIn()) {
            return String.format(
                    "[已登录] 用户 ID: %s | 用户名: %s | 登录状态: 正常",
                    state.getUserId(), state.getUserName()
            );
        }

        return "[未登录] 当前用户尚未登录，需要输入账号和密码才能继续操作（如：下单）。请引导用户提供账号和密码。";
    }

    /**
     * 模拟登录。
     * <p>
     * 仅用于演示，校验固定账号密码。登录成功后将用户信息持久化到会话状态。
     *
     * @param account  用户名（测试账号: zhangsan / lisi）
     * @param password 密码（测试密码: 123456）
     */
    @Tool(description = "执行用户登录。参数 account: 用户账号名；参数 password: 登录密码。登录成功后将用户信息存入会话，后续操作无需重复登录。")
    public String login(String account, String password) {
        SessionState state = SessionContext.currentState();

        // 校验账号密码
        if (!MOCK_USER_ACCOUNT.equals(account) && !"lisi".equals(account)) {
            return String.format(
                    "[登录失败] 账号 '%s' 不存在。可用测试账号: zhangsan / lisi，密码均为 123456",
                    account
            );
        }

        if (!MOCK_USER_PASSWORD.equals(password)) {
            return "[登录失败] 密码错误，请重试。提示: 测试密码为 123456";
        }

        // 登录成功 —— 写入会话状态
        String userName = MOCK_USER_ACCOUNT.equals(account) ? MOCK_USER_NAME : "李四";
        state.setLoggedIn(true);
        state.setUserId(account);
        state.setUserName(userName);

        return String.format(
                "[登录成功] 欢迎回来，%s！(账号: %s) 现在可以继续点单了。",
                userName, account
        );
    }
}
