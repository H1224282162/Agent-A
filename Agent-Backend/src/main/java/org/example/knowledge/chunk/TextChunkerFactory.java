package org.example.knowledge.chunk;

import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 文本分块器工厂
 */
@Component
public class TextChunkerFactory {

    private final List<TextChunker> chunkers;

    public TextChunkerFactory(List<TextChunker> chunkers) {
        this.chunkers = chunkers;
    }

    /**
     * 根据分块策略获取对应分块器
     *
     * @param strategy 分块策略：fixed / sliding / paragraph
     * @return 文本分块器
     */
    public TextChunker getChunker(String strategy) {
        if (strategy == null || strategy.isEmpty()) {
            throw new IllegalArgumentException("分块策略不能为空");
        }
        return chunkers.stream()
                .filter(chunker -> chunker.supports(strategy))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("不支持的分块策略: " + strategy));
    }
}
