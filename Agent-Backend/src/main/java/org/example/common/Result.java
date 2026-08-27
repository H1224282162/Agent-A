package org.example.common;

import java.io.Serializable;

/**
 * 统一 API 返回结果包装类。
 * <p>
 * 所有 Controller 接口（除 SSE 流式接口外）统一返回 {@code Result<T>}，
 * 前端通过 code 判断请求是否成功：
 * <ul>
 *   <li>{@code code == 200}：成功，取 data 渲染</li>
 *   <li>{@code code != 200}：失败，取 message 提示错误</li>
 * </ul>
 *
 * @param <T> 业务数据类型
 */
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功状态码 */
    public static final int SUCCESS_CODE = 200;

    /** 通用失败状态码 */
    public static final int FAIL_CODE = 500;

    /** 状态码：200 成功，其余为失败 */
    private int code;

    /** 提示信息：成功时为操作说明，失败时为错误原因 */
    private String message;

    /** 业务数据 */
    private T data;

    public Result() {
    }

    public Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    // ──────────── 静态工厂方法 ────────────

    /** 成功，无数据 */
    public static <T> Result<T> success() {
        return new Result<>(SUCCESS_CODE, "操作成功", null);
    }

    /** 成功，携带数据 */
    public static <T> Result<T> success(T data) {
        return new Result<>(SUCCESS_CODE, "操作成功", data);
    }

    /** 成功，自定义提示 + 数据 */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(SUCCESS_CODE, message, data);
    }

    /** 失败，默认 500 状态码 */
    public static <T> Result<T> fail(String message) {
        return new Result<>(FAIL_CODE, message, null);
    }

    /** 失败，自定义状态码 */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }

    // ──────────── getter / setter ────────────

    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
}
