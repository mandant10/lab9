package com.example.ai_cos.config;

import com.example.ai_cos.service.DatabaseTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider databaseToolCallbacks(DatabaseTools databaseTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(databaseTools)
                .build();
    }
}
