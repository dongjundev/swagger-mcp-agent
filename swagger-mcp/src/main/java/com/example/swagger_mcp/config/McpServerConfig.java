package com.example.swagger_mcp.config;

import com.example.swagger_mcp.tool.SwaggerTools;
import io.modelcontextprotocol.server.McpStatelessServerFeatures;
import io.modelcontextprotocol.spec.McpSchema;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.util.List;

@Configuration
public class McpServerConfig {

    @Bean
    public ToolCallbackProvider toolCallbackProvider(SwaggerTools swaggerTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(swaggerTools)
                .build();
    }

    @Bean
    public List<McpStatelessServerFeatures.AsyncPromptSpecification> searchApisPrompt() {
        McpSchema.Prompt prompt = new McpSchema.Prompt(
                "search-apis",
                "Search relevant APIs from one of the registered services",
                List.of(
                        new McpSchema.PromptArgument("serviceName", "The name of the service", true),
                        new McpSchema.PromptArgument("apiDesc", "Description of the API to search", true)));

        return List.of(new McpStatelessServerFeatures.AsyncPromptSpecification(prompt, (context, request) -> {
            String serviceName = (String) request.arguments().get("serviceName");
            String apiDesc = (String) request.arguments().get("apiDesc");
            String text = """
                    Find the API of service "%s" that matches this description: %s

                    Use the swagger-mcp tools to answer:
                    1. getApiList with a keyword to find candidate APIs of the service.
                    2. getApiDetail for the best match.
                    3. getComponentSchema for every $ref in the detail.
                    Then summarize the API: method, path, parameters, request body and response."""
                    .formatted(serviceName, apiDesc);

            return Mono.just(new McpSchema.GetPromptResult(
                    "Search APIs of " + serviceName + " matching '" + apiDesc + "'",
                    List.of(new McpSchema.PromptMessage(McpSchema.Role.USER, new McpSchema.TextContent(text)))));
        }));
    }
}
