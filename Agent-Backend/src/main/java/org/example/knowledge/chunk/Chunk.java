package org.example.knowledge.chunk;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 文本分块结果
 */
@Data
public class Chunk {

    /**
     * 分块序号
     */
    private int index;

    /**
     * 分块文本内容
     */
    private String content;

    /**
     * 元数据：起始位置、结束位置、页码、段落号等
     */
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Token 数量估算（粗略按中文字符数估算）
     */
    private int tokenCount;

    public Chunk() {
    }

    public Chunk(int index, String content) {
        this.index = index;
        this.content = content;
    }

    public Chunk(int index, String content, Map<String, Object> metadata) {
        this.index = index;
        this.content = content;
        this.metadata = metadata;
    }
}
