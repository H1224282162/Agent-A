package org.example;

import org.example.config.KnowledgeConfig;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.ai.model.openai.autoconfigure.OpenAiChatAutoConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * 启动类
 */
@SpringBootApplication(exclude = {OpenAiChatAutoConfiguration.class})
@MapperScan("org.example.mapper")
@EnableConfigurationProperties(KnowledgeConfig.class)
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }
}
