package org.example.knowledge.parse;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文档解析器工厂
 */
@Component
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParserFactory(List<DocumentParser> parsers) {
        this.parsers = parsers;
    }

    /**
     * 根据文件类型获取对应解析器
     *
     * @param fileType 文件类型小写扩展名
     * @return 文档解析器
     */
    public DocumentParser getParser(String fileType) {
        if (fileType == null) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        String type = fileType.toLowerCase();
        return parsers.stream()
                .filter(parser -> parser.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的文件类型: " + fileType));
    }
}
