package org.example.knowledge.chunk;

import org.example.model.KnowledgeBase;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 按段落分块器
 * <p>
 * 先按空行（\n\n）切分段落；若段落超过 chunkSize，则使用固定长度二次切分。
 */
@Component
public class ParagraphChunker implements TextChunker {

    @Override
    public boolean supports(String strategy) {
        return "paragraph".equalsIgnoreCase(strategy);
    }

    @Override
    public List<Chunk> chunk(String text, KnowledgeBase kb) {
        int chunkSize = kb.getChunkSize() != null && kb.getChunkSize() > 0 ? kb.getChunkSize() : 512;
        List<Chunk> chunks = new ArrayList<>();

        String[] paragraphs = text.split("\n\\s*\n");
        int index = 0;
        int paragraphNo = 0;

        for (String paragraph : paragraphs) {
            paragraphNo++;
            paragraph = paragraph.trim().replaceAll("\s+", " ");
            if (paragraph.isEmpty()) {
                continue;
            }

            if (paragraph.length() <= chunkSize) {
                Chunk chunk = new Chunk(index++, paragraph);
                chunk.getMetadata().put("paragraphNo", paragraphNo);
                chunk.setTokenCount(estimateTokenCount(paragraph));
                chunks.add(chunk);
            } else {
                // 长段落二次切分
                int position = 0;
                int length = paragraph.length();
                while (position < length) {
                    int end = Math.min(position + chunkSize, length);
                    String content = paragraph.substring(position, end).trim();
                    if (!content.isEmpty()) {
                        Chunk chunk = new Chunk(index++, content);
                        chunk.getMetadata().put("paragraphNo", paragraphNo);
                        chunk.getMetadata().put("subChunk", true);
                        chunk.setTokenCount(estimateTokenCount(content));
                        chunks.add(chunk);
                    }
                    position = end;
                }
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
