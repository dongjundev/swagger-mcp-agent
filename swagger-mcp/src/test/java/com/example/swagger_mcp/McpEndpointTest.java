package com.example.swagger_mcp;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class McpEndpointTest {

    @LocalServerPort
    private int port;

    @Test
    void servesSearchApisPrompt() {
        String response = RestClient.create("http://localhost:" + port).post()
                .uri("/mcp")
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM)
                .body("""
                        {"jsonrpc":"2.0","id":1,"method":"prompts/get","params":{"name":"search-apis",
                         "arguments":{"serviceName":"ms-order","apiDesc":"cancel an order"}}}""")
                .retrieve()
                .body(String.class);

        assertThat(response).contains("ms-order", "cancel an order", "getApiList");
    }
}
