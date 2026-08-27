package org.example.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 知识库业务配置
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.knowledge")
public class KnowledgeConfig {

    /**
     * 知识库文件本地存储目录
     */
    private String fileStorePath = "./upload/knowledge";

    /**
     * 支持的文件扩展名白名单
     */
    private List<String> allowedExtensions = List.of("txt", "md", "markdown", "pdf", "doc", "docx");

    /**
     * ES 索引名前缀
     */
    private String indexPrefix = "kb";

}
