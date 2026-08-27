package org.example.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.ai.chat.messages.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Repository
public class RedisChatMemoryRepository implements ChatMemoryRepository {

    private static final String SESSION_KEY_PREFIX = "ai:chat:session:";
    private static final String SESSION_SET_KEY = "ai:chat:sessions";
    private static final long EXPIRE_HOURS = 72;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    public RedisChatMemoryRepository(@Qualifier("redisStringTemplate") RedisTemplate<String, String> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String getSessionKey(String conversationId) {
        return SESSION_KEY_PREFIX + conversationId;
    }

    @Override
    public List<String> findConversationIds() {
        Set<String> members = redisTemplate.opsForSet().members(SESSION_SET_KEY);
        if (members == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(members);
    }

    @Override
    public List<Message> findByConversationId(String conversationId) {
        String key = getSessionKey(conversationId);
        String json = redisTemplate.opsForValue().get(key);
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        try {
            ArrayNode arrayNode = (ArrayNode) objectMapper.readTree(json);
            ArrayList<Message> messages = new ArrayList<>();
            for (JsonNode node : arrayNode) {
                messages.add(deserializeMessage(node));
            }
            return messages;
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to deserialize messages for conversation: " + conversationId, e);
        }
    }

    @Override
    public void saveAll(String conversationId, List<Message> messages) {
        String key = getSessionKey(conversationId);
        try {
            ArrayNode arrayNode = objectMapper.createArrayNode();
            for (Message message : messages) {
                arrayNode.add(serializeMessage(message));
            }
            String json = objectMapper.writeValueAsString(arrayNode);
            redisTemplate.opsForValue().set(key, json, EXPIRE_HOURS, TimeUnit.HOURS);
            redisTemplate.opsForSet().add(SESSION_SET_KEY, conversationId);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save messages for conversation: " + conversationId, e);
        }
    }

    @Override
    public void deleteByConversationId(String conversationId) {
        String key = getSessionKey(conversationId);
        redisTemplate.delete(key);
        redisTemplate.opsForSet().remove(SESSION_SET_KEY, conversationId);
    }

    private ObjectNode serializeMessage(Message message) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("messageType", message.getMessageType().name());
        node.put("text", message.getText());

        if (message instanceof AssistantMessage assistantMessage) {
            if (assistantMessage.getToolCalls() != null && !assistantMessage.getToolCalls().isEmpty()) {
                ArrayNode toolCallsNode = objectMapper.createArrayNode();
                for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                    ObjectNode tcNode = objectMapper.createObjectNode();
                    tcNode.put("id", toolCall.id());
                    tcNode.put("type", toolCall.type());
                    tcNode.put("name", toolCall.name());
                    tcNode.put("arguments", toolCall.arguments());
                    toolCallsNode.add(tcNode);
                }
                node.set("toolCalls", toolCallsNode);
            }
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            if (toolResponseMessage.getResponses() != null && !toolResponseMessage.getResponses().isEmpty()) {
                ArrayNode responsesNode = objectMapper.createArrayNode();
                for (ToolResponseMessage.ToolResponse response : toolResponseMessage.getResponses()) {
                    ObjectNode rNode = objectMapper.createObjectNode();
                    rNode.put("id", response.id());
                    rNode.put("name", response.name());
                    rNode.put("responseData", response.responseData());
                    responsesNode.add(rNode);
                }
                node.set("responses", responsesNode);
            }
        }

        return node;
    }

    private Message deserializeMessage(JsonNode node) {
        String type = node.get("messageType").asText();
        String text = node.has("text") ? node.get("text").asText() : "";

        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(text);
            case SYSTEM -> new SystemMessage(text);
            case ASSISTANT -> {
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();
                if (node.has("toolCalls")) {
                    for (JsonNode tcNode : node.get("toolCalls")) {
                        toolCalls.add(new AssistantMessage.ToolCall(
                                tcNode.get("id").asText(),
                                tcNode.get("type").asText(),
                                tcNode.get("name").asText(),
                                tcNode.get("arguments").asText()
                        ));
                    }
                }
                yield AssistantMessage.builder()
                        .content(text)
                        .toolCalls(toolCalls)
                        .build();
            }
            case TOOL -> {
                List<ToolResponseMessage.ToolResponse> responses = new ArrayList<>();
                if (node.has("responses")) {
                    for (JsonNode rNode : node.get("responses")) {
                        responses.add(new ToolResponseMessage.ToolResponse(
                                rNode.get("id").asText(),
                                rNode.get("name").asText(),
                                rNode.get("responseData").asText()
                        ));
                    }
                }
                yield ToolResponseMessage.builder()
                        .responses(responses)
                        .build();
            }
        };
    }

}
