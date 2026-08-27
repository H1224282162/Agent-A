package org.example.knowledge.parse;

import java.io.IOException;
import java.io.InputStream;

/**
 * 文档解析器接口
 */
public interface DocumentParser {

    /**
     * 是否支持该文件类型
     *
     * @param fileType 文件类型小写扩展名，如 txt、pdf、docx
     * @return true 表示支持
     */
    boolean supports(String fileType);

    /**
     * 解析文件输入流
     *
     * @param inputStream 文件输入流
     * @return 解析结果（文本 + 元数据）
     * @throws IOException 解析异常
     */
    ParseResult parse(InputStream inputStream) throws IOException;
}
