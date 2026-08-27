package org.example.knowledge.parse;

import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Word 文档解析器（支持 .docx）
 * <p>
 * 如需支持老版本 .doc 格式，请额外引入 org.apache.poi:poi-scratchpad 依赖。
 */
@Component
public class WordDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileType) {
        return "docx".equalsIgnoreCase(fileType) || "doc".equalsIgnoreCase(fileType);
    }

    @Override
    public ParseResult parse(InputStream inputStream) throws IOException {
        try (XWPFDocument docx = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(docx)) {
            String text = extractor.getText();
            ParseResult result = new ParseResult(text);
            result.getMetadata().put("parser", "WordDocumentParser");
            result.getMetadata().put("format", "docx");
            return result;
        }
    }
}
