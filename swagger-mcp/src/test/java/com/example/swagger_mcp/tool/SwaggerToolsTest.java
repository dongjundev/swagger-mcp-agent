package com.example.swagger_mcp.tool;

import com.example.swagger_mcp.client.SwaggerCenterClient;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SwaggerToolsTest {

    private final SwaggerCenterClient client = mock(SwaggerCenterClient.class);
    private final SwaggerTools tools = new SwaggerTools(client);

    @Test
    void getComponentSchemaAcceptsRefPath() {
        tools.getComponentSchema("ms-order", "#/components/schemas/OrderDto");

        verify(client).getComponentSchema("ms-order", "OrderDto");
    }

    @Test
    void getComponentSchemaAcceptsPlainName() {
        tools.getComponentSchema("ms-order", "OrderDto");

        verify(client).getComponentSchema("ms-order", "OrderDto");
    }
}
