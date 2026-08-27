package org.example.common;

/**
 * 聊天消息视图对象 —— 用于向前端返回会话历史。
 *
 * @param role    消息角色：user / assistant
 * @param content 消息文本内容
 */
public record ChatMessageVO(String role, String content) {}
