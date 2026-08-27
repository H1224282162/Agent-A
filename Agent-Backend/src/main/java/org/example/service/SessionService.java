package org.example.service;

import org.example.common.ChatMessageVO;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 会话服务 —— 负责会话 id 生成、历史会话查询与历史消息读取。
 * <p>
 * 会话记忆统一存放在 Redis（见 {@link ChatMemoryRepository}），
 * 会话 id 采用 {@code {agentCode}_{毫秒时间戳}_{随机串}} 的格式，
 * 既保证唯一，又可通过字符串倒序实现「最近优先」排序。
 */
@Service
public class SessionService {

    private final ChatMemoryRepository chatMemoryRepository;

    public SessionService(ChatMemoryRepository chatMemoryRepository) {
        this.chatMemoryRepository = chatMemoryRepository;
    }

    /**
     * 生成新会话 id。
     *
     * @param agentCode Agent 编码
     * @return 形如 {@code order_helper_1724736123456_ab3f2c} 的会话 id
     */
    public String newSessionId(String agentCode) {
        String random = UUID.randomUUID().toString().replace("-", "").substring(0, 6);
        return agentCode + "_" + System.currentTimeMillis() + "_" + random;
    }

    /**
     * 查询某 Agent 的历史会话 id 列表（按时间倒序，最近优先）。
     */
    public List<String> listSessions(String agentCode) {
        String prefix = agentCode + "_";
        return chatMemoryRepository.findConversationIds().stream()
                .filter(id -> id != null && id.startsWith(prefix))
                .sorted(Comparator.reverseOrder())
                .toList();
    }

    /**
     * 查询某会话的历史消息，仅返回 user / assistant 文本，
     * 过滤掉工具调用产生的中间消息（含 toolCalls 的 assistant 消息与 tool 响应）。
     */
    public List<ChatMessageVO> history(String sessionId) {
        return chatMemoryRepository.findByConversationId(sessionId).stream()
                .filter(this::isTextMessage)
                .map(m -> new ChatMessageVO(
                        m.getMessageType() == MessageType.USER ? "user" : "assistant",
                        m.getText()))
                .toList();
    }

    private boolean isTextMessage(Message m) {
        MessageType type = m.getMessageType();
        if (type != MessageType.USER && type != MessageType.ASSISTANT) {
            return false;
        }
        return m.getText() != null && !m.getText().isBlank();
    }
}
