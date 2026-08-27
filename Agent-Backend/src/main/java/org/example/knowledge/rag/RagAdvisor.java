package org.example.knowledge.rag;

import lombok.extern.slf4j.Slf4j;
import org.example.knowledge.vector.RetrievalResult;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * RAG Advisor —— 在调用大模型前召回知识库片段并拼接到 System Prompt
 */
@Slf4j
@Component
public class RagAdvisor implements CallAdvisor, StreamAdvisor {

    public static final String AGENT_CODE_KEY = "agentCode";

    private final KnowledgeRetrievalService retrievalService;

    public RagAdvisor(KnowledgeRetrievalService retrievalService) {
        this.retrievalService = retrievalService;
    }

    @Override
    public String getName() {
        return "RagAdvisor";
    }

    @Override
    public int getOrder() {
        // 在 MessageChatMemoryAdvisor 之后执行
        return 100;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest request, CallAdvisorChain chain) {
        ChatClientRequest enriched = enrichRequest(request);
        return chain.nextCall(enriched);
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(ChatClientRequest request, StreamAdvisorChain chain) {
        ChatClientRequest enriched = enrichRequest(request);
        return chain.nextStream(enriched);
    }

    /**
     * 在用户请求前拼接知识库上下文到 System Prompt
     */
    private ChatClientRequest enrichRequest(ChatClientRequest request) {
        Map<String, Object> context = request.context();
        String agentCode = context != null ? (String) context.get(AGENT_CODE_KEY) : null;
        if (agentCode == null || agentCode.isEmpty()) {
            return request;
        }

        String query = null;
        if (request.prompt().getUserMessage() != null) {
            query = request.prompt().getUserMessage().getText();
        }
        if (query == null || query.isEmpty()) {
            return request;
        }

        try {
            List<RetrievalResult> results = retrievalService.retrieve(agentCode, query);
            if (results.isEmpty()) {
                return request;
            }

            String contextText = retrievalService.formatContext(results);
            Prompt newPrompt = request.prompt().augmentSystemMessage(contextText);

            return request.mutate()
                    .prompt(newPrompt)
                    .build();
        } catch (Exception e) {
            log.error("RAG 召回异常，agentCode={}, query={}", agentCode, query, e);
            return request;
        }
    }
}
