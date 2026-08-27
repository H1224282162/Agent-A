package org.example.knowledge.parse;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Markdown 解析器
 */
@Component
public class MarkdownParser implements DocumentParser {

    private final Parser parser = Parser.builder().build();
    private final TextContentRenderer renderer = TextContentRenderer.builder().build();

    @Override
    public boolean supports(String fileType) {
        return "md".equalsIgnoreCase(fileType) || "markdown".equalsIgnoreCase(fileType);
    }

    @Override
    public ParseResult parse(InputStream inputStream) throws IOException {
        String markdown = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        Node document = parser.parse(markdown);
        String text = renderer.render(document);
        ParseResult result = new ParseResult(text);
        result.getMetadata().put("parser", "MarkdownParser");
        return result;
    }
}
