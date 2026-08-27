package org.example.knowledge.chunk;

import org.example.model.KnowledgeBase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 滑动窗口分块器
 */
@Component
public class SlidingWindowChunker implements TextChunker {

    @Override
    public boolean supports(String strategy) {
        return "sliding".equalsIgnoreCase(strategy);
    }

    @Override
    public List<Chunk> chunk(String text, KnowledgeBase kb) {
        int chunkSize = kb.getChunkSize() != null && kb.getChunkSize() > 0 ? kb.getChunkSize() : 512;
        int overlap = kb.getChunkOverlap() != null && kb.getChunkOverlap() >= 0 ? kb.getChunkOverlap() : 50;
        int step = Math.max(1, chunkSize - overlap);

        List<Chunk> chunks = new ArrayList<>();
        int index = 0;
        int position = 0;
        int length = text.length();

        while (position < length) {
            int end = Math.min(position + chunkSize, length);
            String content = text.substring(position, end).trim();
            if (!content.isEmpty()) {
                Chunk chunk = new Chunk(index++, content);
                chunk.getMetadata().put("start", position);
                chunk.getMetadata().put("end", end);
                chunk.setTokenCount(estimateTokenCount(content));
                chunks.add(chunk);
            }
            position += step;
            // 避免最后一个 chunk 极短
            if (position >= length && end < length) {
                break;
            }
        }

        return chunks;
    }

    private int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        long chineseCount = content.chars().filter(c -> c >= 0x4E00 && c <= 0x9FA5).count();
        long otherCount = content.length() - chineseCount;
        return (int) Math.ceil(chineseCount * 0.75 + otherCount * 0.3);
    }
}
