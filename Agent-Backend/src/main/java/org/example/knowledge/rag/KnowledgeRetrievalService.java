package org.example.knowledge.rag;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.example.knowledge.vector.ElasticsearchVectorStore;
import org.example.knowledge.vector.RetrievalResult;
import org.example.model.AgentDef;
import org.example.model.AgentKnowledge;
import org.example.model.KnowledgeBase;
import org.example.service.IAgentDefService;
import org.example.service.IAgentKnowledgeService;
import org.example.service.IKnowledgeBaseService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 知识库检索服务
 * <p>
 * 根据 Agent 编码查询绑定的启用知识库，执行向量化召回。
 */
@Slf4j
@Service
public class KnowledgeRetrievalService {

    private final IAgentDefService agentDefService;
    private final IAgentKnowledgeService agentKnowledgeService;
    private final IKnowledgeBaseService knowledgeBaseService;
    private final ElasticsearchVectorStore vectorStore;

    public KnowledgeRetrievalService(IAgentDefService agentDefService,
                                     IAgentKnowledgeService agentKnowledgeService,
                                     IKnowledgeBaseService knowledgeBaseService,
                                     ElasticsearchVectorStore vectorStore) {
        this.agentDefService = agentDefService;
        this.agentKnowledgeService = agentKnowledgeService;
        this.knowledgeBaseService = knowledgeBaseService;
        this.vectorStore = vectorStore;
    }

    /**
     * 根据 Agent 编码检索相关知识库片段
     *
     * @param agentCode Agent 编码
     * @param query     用户查询
     * @return 检索结果列表（已按相似度排序）
     */
    public List<RetrievalResult> retrieve(String agentCode, String query) {
        if (agentCode == null || agentCode.isEmpty() || query == null || query.isEmpty()) {
            return List.of();
        }

        // 根据 agentCode 获取 agentId
        AgentDef agentDef = agentDefService.getOne(
                new LambdaQueryWrapper<AgentDef>().eq(AgentDef::getAgentCode, agentCode)
        );
        if (agentDef == null) {
            log.warn("Agent {} 不存在", agentCode);
            return List.of();
        }

        // 查询 Agent 绑定的启用知识库
        List<AgentKnowledge> bindings = agentKnowledgeService.listEnabledByAgentId(agentDef.getId());
        if (bindings == null || bindings.isEmpty()) {
            log.debug("Agent {} 未绑定任何知识库", agentCode);
            return List.of();
        }

        List<Long> kbIds = bindings.stream()
                .map(AgentKnowledge::getKbId)
                .distinct()
                .toList();

        List<KnowledgeBase> knowledgeBases = knowledgeBaseService.listByIds(kbIds);
        if (knowledgeBases == null || knowledgeBases.isEmpty()) {
            return List.of();
        }

        List<RetrievalResult> allResults = new ArrayList<>();
        for (KnowledgeBase kb : knowledgeBases) {
            if (kb.getStatus() == null || kb.getStatus() != 1) {
                continue;
            }
            String indexName = kb.getVectorIndexName();
            int topK = kb.getTopK() != null && kb.getTopK() > 0 ? kb.getTopK() : 5;
            double threshold = kb.getSimilarityThreshold() != null ? kb.getSimilarityThreshold() : 0.75;

            try {
                List<RetrievalResult> results = vectorStore.similaritySearch(indexName, query, topK, threshold, kb.getId());
                allResults.addAll(results);
            } catch (Exception e) {
                log.error("知识库 {} 检索异常: {}", kb.getKbName(), e.getMessage(), e);
            }
        }

        // 全局按相似度降序，取 Top-K（使用第一个知识库的 topK 作为全局限制）
        int globalTopK = knowledgeBases.stream()
                .map(KnowledgeBase::getTopK)
                .filter(topK -> topK != null && topK > 0)
                .findFirst()
                .orElse(5);

        return allResults.stream()
                .sorted(Comparator.comparingDouble(RetrievalResult::getScore).reversed())
                .limit(globalTopK)
                .collect(Collectors.toList());
    }

    /**
     * 将检索结果格式化为 Prompt 上下文
     */
    public String formatContext(List<RetrievalResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n## 知识库参考\n");
        sb.append("以下是与用户问题相关的参考内容，请在回答时优先参考：\n\n");
        for (int i = 0; i < results.size(); i++) {
            RetrievalResult r = results.get(i);
            sb.append("[").append(i + 1).append("] ");
            if (r.getMetadata() != null && r.getMetadata().get("paragraphNo") != null) {
                sb.append("(段落 ").append(r.getMetadata().get("paragraphNo")).append(") ");
            }
            sb.append(r.getContent().trim());
            if (i < results.size() - 1) {
                sb.append("\n\n");
            }
        }
        return sb.toString();
    }
}
