package org.example.knowledge.vector;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.KnnSearch;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.DeleteByQueryResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorProperty;
import co.elastic.clients.elasticsearch._types.mapping.DenseVectorSimilarity;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import lombok.extern.slf4j.Slf4j;
import org.example.config.model.ModelRegistry;
import org.example.model.KnowledgeChunk;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Elasticsearch 向量存储服务
 * <p>
 * 负责 ES 索引创建、chunk 向量写入、删除、相似度检索。
 */
@Slf4j
@Service
public class ElasticsearchVectorStore {

    /**
     * 默认向量维度（OpenAI text-embedding-3-small / Moonshot v1 embedding）
     */
    public static final int DEFAULT_DIMS = 1536;

    private final ElasticsearchClient esClient;
    private final ModelRegistry modelRegistry;

    public ElasticsearchVectorStore(ElasticsearchClient esClient, ModelRegistry modelRegistry) {
        this.esClient = esClient;
        this.modelRegistry = modelRegistry;
    }

    /**
     * 获取 Embedding 模型维度
     */
    public int getDimensions() {
        int dims = modelRegistry.defaultEmbeddingModel().dimensions();
        return dims > 0 ? dims : DEFAULT_DIMS;
    }

    /**
     * 创建 ES 向量索引
     *
     * @param indexName 索引名
     * @param dims      向量维度
     */
    public void createIndex(String indexName, int dims) {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("ES 索引已存在，跳过创建: {}", indexName);
                return;
            }

            DenseVectorProperty denseVector = DenseVectorProperty.of(d -> d
                    .dims(dims)
                    .index(true)
                    .similarity(DenseVectorSimilarity.Cosine)
            );

            TypeMapping mapping = TypeMapping.of(m -> m.properties("kb_id", Property.of(p -> p.long_(l -> l)))
                    .properties("doc_id", Property.of(p -> p.long_(l -> l)))
                    .properties("chunk_id", Property.of(p -> p.long_(l -> l)))
                    .properties("content", Property.of(p -> p.text(t -> t.analyzer("ik_smart"))))
                    .properties("metadata", Property.of(p -> p.object(o -> o)))
                    .properties("embedding", Property.of(p -> p.denseVector(denseVector)))
            );

            CreateIndexRequest request = CreateIndexRequest.of(r -> r
                    .index(indexName)
                    .mappings(mapping)
            );

            CreateIndexResponse response = esClient.indices().create(request);
            if (response.acknowledged()) {
                log.info("ES 索引创建成功: {}", indexName);
            } else {
                log.warn("ES 索引创建未完全确认: {}", indexName);
            }
        } catch (IOException e) {
            throw new RuntimeException("创建 ES 索引失败: " + indexName, e);
        }
    }

    /**
     * 批量写入 chunk 向量
     *
     * @param indexName 索引名
     * @param chunks    chunk 列表（需包含 kbId、docId、id、content）
     * @return ES 文档 ID 列表，顺序与 chunks 一致
     */
    public List<String> batchAdd(String indexName, List<KnowledgeChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            BulkRequest.Builder bulkBuilder = new BulkRequest.Builder();
            for (KnowledgeChunk chunk : chunks) {
                Map<String, Object> doc = buildEsDocument(chunk);
                bulkBuilder.operations(op -> op.index(idx -> idx
                        .index(indexName)
                        .document(doc)
                ));
            }

            BulkResponse response = esClient.bulk(bulkBuilder.build());
            if (response.errors()) {
                List<String> errors = response.items().stream()
                        .filter(item -> item.error() != null)
                        .map(item -> item.error().reason())
                        .collect(Collectors.toList());
                throw new RuntimeException("批量写入 ES 失败: " + errors);
            }

            List<String> esDocIds = new ArrayList<>();
            for (BulkResponseItem item : response.items()) {
                esDocIds.add(item.id());
            }
            return esDocIds;
        } catch (IOException e) {
            throw new RuntimeException("批量写入 ES 异常", e);
        }
    }

    /**
     * 根据 ES 文档 ID 删除向量
     */
    public void deleteByEsDocId(String indexName, String esDocId) {
        try {
            esClient.delete(d -> d.index(indexName).id(esDocId));
        } catch (IOException e) {
            throw new RuntimeException("删除 ES 文档失败: " + esDocId, e);
        }
    }

    /**
     * 根据文档 ID 删除该文档下的所有向量
     */
    public void deleteByDocId(String indexName, Long docId) {
        try {
            DeleteByQueryRequest request = DeleteByQueryRequest.of(r -> r
                    .index(indexName)
                    .query(q -> q.term(t -> t.field("doc_id").value(docId)))
            );
            DeleteByQueryResponse response = esClient.deleteByQuery(request);
            log.info("删除文档 {} 的向量记录数: {}", docId, response.deleted());
        } catch (IOException e) {
            throw new RuntimeException("按文档 ID 删除向量失败: " + docId, e);
        }
    }

    /**
     * 删除整个索引
     */
    public void deleteIndex(String indexName) {
        try {
            boolean exists = esClient.indices().exists(e -> e.index(indexName)).value();
            if (!exists) {
                return;
            }
            DeleteIndexRequest request = DeleteIndexRequest.of(r -> r.index(indexName));
            DeleteIndexResponse response = esClient.indices().delete(request);
            if (response.acknowledged()) {
                log.info("ES 索引删除成功: {}", indexName);
            }
        } catch (IOException e) {
            throw new RuntimeException("删除 ES 索引失败: " + indexName, e);
        }
    }

    /**
     * 相似度检索
     *
     * @param indexName 索引名（支持多个知识库的索引，此处传入单个索引名；多库检索由调用方循环）
     * @param query     查询文本
     * @param topK      返回数量
     * @param threshold 相似度阈值
     * @param kbId      知识库 ID 过滤（可选）
     * @return 检索结果列表
     */
    public List<RetrievalResult> similaritySearch(String indexName, String query, int topK, double threshold, Long kbId) {
        try {
            List<Float> vector = embed(query);
            int numCandidates = Math.max(topK * 2, 50);

            KnnSearch.Builder knnBuilder = new KnnSearch.Builder()
                    .field("embedding")
                    .queryVector(vector)
                    .k(topK)
                    .numCandidates(numCandidates);

            if (kbId != null) {
                knnBuilder.filter(f -> f.term(t -> t.field("kb_id").value(kbId)));
            }

            SearchRequest request = SearchRequest.of(r -> r
                    .index(indexName)
                    .knn(knnBuilder.build())
                    .size(topK)
            );

            SearchResponse<Map> response = esClient.search(request, Map.class);
            List<RetrievalResult> results = new ArrayList<>();

            response.hits().hits().forEach(hit -> {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    return;
                }
                Double score = hit.score();
                if (score == null || score < threshold) {
                    return;
                }

                RetrievalResult result = new RetrievalResult();
                result.setChunkId(getLong(source, "chunk_id"));
                result.setDocId(getLong(source, "doc_id"));
                result.setKbId(getLong(source, "kb_id"));
                result.setContent(getString(source, "content"));
                result.setScore(score);
                Object metadata = source.get("metadata");
                if (metadata instanceof Map) {
                    result.setMetadata((Map<String, Object>) metadata);
                }
                results.add(result);
            });

            return results;
        } catch (IOException e) {
            throw new RuntimeException("ES 相似度检索异常", e);
        }
    }

    /**
     * 生成查询向量
     */
    public List<Float> embed(String text) {
        float[] floats = modelRegistry.defaultEmbeddingModel().embed(text);
        if (floats == null || floats.length == 0) {
            throw new RuntimeException("Embedding 生成失败");
        }
        List<Float> result = new ArrayList<>(floats.length);
        for (float f : floats) {
            result.add(f);
        }
        return result;
    }

    private Map<String, Object> buildEsDocument(KnowledgeChunk chunk) {
        Map<String, Object> doc = new HashMap<>();
        doc.put("kb_id", chunk.getKbId());
        doc.put("doc_id", chunk.getDocId());
        doc.put("chunk_id", chunk.getId());
        doc.put("content", chunk.getContent());

        // metadata 解析
        Map<String, Object> metadata = new HashMap<>();
        if (chunk.getMetadata() != null && !chunk.getMetadata().isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                metadata = mapper.readValue(chunk.getMetadata(), Map.class);
            } catch (Exception e) {
                metadata.put("raw", chunk.getMetadata());
            }
        }
        doc.put("metadata", metadata);

        // 生成向量
        List<Float> vector = embed(chunk.getContent());
        doc.put("embedding", vector);

        return doc;
    }

    private Long getLong(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return Long.valueOf(value.toString());
    }

    private String getString(Map<String, Object> source, String key) {
        Object value = source.get(key);
        return value == null ? null : value.toString();
    }
}
