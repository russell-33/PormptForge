package com.max.aicoder.config;

import com.max.aicoder.ai.memory.CompressingService;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "compress.chat-model")
@Data
public class CompressChatModelConfig {

    private String baseUrl;
    private String apiKey;
    private String modelName;
    private Integer maxTokens = 512;
    private Double temperature = 0.3;

    @Bean
    public ChatModel compressChatModel() {
        return OpenAiChatModel.builder()
                .apiKey(apiKey)
                .baseUrl(baseUrl)
                .modelName(modelName)
                .maxTokens(maxTokens)
                .temperature(temperature)
                .build();
    }

    @Bean
    public CompressingService compressingService(ChatModel compressChatModel) {
        return new CompressingService(compressChatModel);
    }
}
