package org.example.knowledge.chunk;

import org.example.model.KnowledgeBase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 固定长度分块器
 */
@Component
public class FixedLengthChunker implements TextChunker {

    @Override
    public boolean supports(String strategy) {
        return "fixed".equalsIgnoreCase(strategy);
    }

    @Override
    public List<Chunk> chunk(String text, KnowledgeBase kb) {
        int chunkSize = kb.getChunkSize() != null && kb.getChunkSize() > 0 ? kb.getChunkSize() : 512;
        List<Chunk> chunks = new ArrayList<>();

        int index = 0;
        int position = 0;
        int length = text.length();

        while (position < length) {
            int end = Math.min(position + chunkSize, length);
            // 尽量在句子或换行处截断
            if (end < length) {
                int newlinePos = text.lastIndexOf('\n', end);
                if (newlinePos > position && end - newlinePos < chunkSize / 4) {
                    end = newlinePos + 1;
                } else {
                    int sentencePos = findSentenceBoundary(text, end);
                    if (sentencePos > position && sentencePos < end) {
                        end = sentencePos + 1;
                    }
                }
            }

            String content = text.substring(position, end).trim();
            if (!content.isEmpty()) {
                Chunk chunk = new Chunk(index++, content);
                chunk.getMetadata().put("start", position);
                chunk.getMetadata().put("end", end);
                chunk.setTokenCount(estimateTokenCount(content));
                chunks.add(chunk);
            }
            position = end;
        }

        return chunks;
    }

    /**
     * 查找句尾标点后的位置
     */
    private int findSentenceBoundary(String text, int around) {
        for (int i = around - 1; i > Math.max(0, around - 50); i--) {
            char c = text.charAt(i);
            if (c == '。' || c == '．' || c == '.' || c == '！' || c == '？' || c == ';' || c == '；') {
                return i;
            }
        }
        return -1;
    }

    /**
     * 粗略估算 Token 数量：中文按 1 字 ≈ 0.75 Token
     */
    private int estimateTokenCount(String content) {
        if (content == null || content.isEmpty()) {
            return 0;
        }
        long chineseCount = content.chars().filter(c -> c >= 0x4E00 && c <= 0x9FA5).count();
        long otherCount = content.length() - chineseCount;
        return (int) Math.ceil(chineseCount * 0.75 + otherCount * 0.3);
    }
}
