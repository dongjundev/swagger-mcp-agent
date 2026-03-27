package com.example.swagger_mcp.config;

import com.example.swagger_mcp.tool.SwaggerTools;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(SwaggerTools swaggerTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(swaggerTools)
                .build();
    }
}
