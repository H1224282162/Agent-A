package org.example.knowledge.parse;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * 文档解析结果
 */
@Data
public class ParseResult {

    /**
     * 解析后的纯文本内容
     */
    private String text;

    /**
     * 元数据：页码、标题、段落、文件类型等
     */
    private Map<String, Object> metadata = new HashMap<>();

    public ParseResult() {
    }

    public ParseResult(String text) {
        this.text = text;
    }

    public ParseResult(String text, Map<String, Object> metadata) {
        this.text = text;
        this.metadata = metadata;
    }
}
