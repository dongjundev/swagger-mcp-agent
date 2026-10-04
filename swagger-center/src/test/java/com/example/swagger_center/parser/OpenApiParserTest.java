package com.example.swagger_center.parser;

import com.example.swagger_center.Fixtures;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OpenApiParserTest {

    private final OpenApiParser parser = new OpenApiParser();
    private final OpenAPI spec30 = parse("spec-3.0.json");
    private final OpenAPI spec31 = parse("spec-3.1.json");

    @Test
    void componentSchemaKeepsEnumValues() {
        JsonNode schema = json(parser.extractComponentSchema(spec30, "PaymentStatus")).at("/schema");

        assertThat(schema.at("/type").asText()).isEqualTo("string");
        assertThat(schema.at("/enum")).extracting(JsonNode::asText).containsExactly("PENDING", "PAID", "CANCELLED");
    }

    @Test
    void componentSchemaKeepsComposition() {
        assertThat(json(parser.extractComponentSchema(spec30, "Payment")).at("/schema/allOf")).hasSize(2);
        assertThat(json(parser.extractComponentSchema(spec30, "PaymentMethod")).at("/schema/oneOf")).hasSize(2);
    }

    @Test
    void componentSchemaKeepsArrayItems() {
        JsonNode schema = json(parser.extractComponentSchema(spec30, "PaymentList")).at("/schema");

        assertThat(schema.at("/items/$ref").asText()).isEqualTo("#/components/schemas/Payment");
    }

    @Test
    void schemaOf31SpecUsesStandardTypeKeyword() {
        JsonNode schema = json(parser.extractComponentSchema(spec31, "OrderDto")).at("/schema");

        assertThat(schema.at("/type").asText()).isEqualTo("object");
        assertThat(schema.at("/properties/id/type").asText()).isEqualTo("integer");
        assertThat(schema.at("/properties/items/items/$ref").asText()).isEqualTo("#/components/schemas/OrderItemDto");
    }

    @Test
    void outputHasNoSwaggerInternalFields() {
        String schema = json(parser.extractComponentSchema(spec31, "OrderDto")).toString();
        String detail = json(parser.extractApiDetail(spec31, "cancelOrder")).toString();

        assertThat(schema + detail).doesNotContain("exampleSetFlag", "\"types\"");
    }

    @Test
    void summaryIncludesTagsAndDeprecation() {
        JsonNode deletePayment = json(parser.extractApiSummaries(spec30).stream()
                .filter(api -> api.operationId().equals("deletePayment"))
                .findFirst().orElseThrow());

        assertThat(deletePayment.at("/tags")).extracting(JsonNode::asText).containsExactly("admin");
        assertThat(deletePayment.at("/deprecated").asBoolean()).isTrue();
    }

    @Test
    void detailIncludesDescription() {
        JsonNode detail = json(parser.extractApiDetail(spec30, "getPayment"));

        assertThat(detail.at("/description").asText()).contains("status=CANCELLED");
    }

    @Test
    void detailInheritsGlobalSecurity() {
        JsonNode detail = json(parser.extractApiDetail(spec30, "getPayment"));

        assertThat(detail.at("/security/0").has("bearerAuth")).isTrue();
        assertThat(detail.at("/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
    }

    @Test
    void detailPrefersOperationSecurityOverGlobal() {
        JsonNode detail = json(parser.extractApiDetail(spec30, "deletePayment"));

        assertThat(detail.at("/security/0").has("adminKey")).isTrue();
        assertThat(detail.at("/securitySchemes/adminKey/name").asText()).isEqualTo("X-Admin-Key");
        assertThat(detail.at("/securitySchemes").has("bearerAuth")).isFalse();
    }

    private static OpenAPI parse(String fixture) {
        return new OpenAPIV3Parser().readContents(Fixtures.read(fixture)).getOpenAPI();
    }

    private static JsonNode json(Object value) {
        return new ObjectMapper().valueToTree(value);
    }
}
