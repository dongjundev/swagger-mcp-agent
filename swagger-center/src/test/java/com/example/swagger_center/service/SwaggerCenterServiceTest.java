package com.example.swagger_center.service;

import com.example.swagger_center.Fixtures;
import com.example.swagger_center.domain.ApiSummary;
import com.example.swagger_center.domain.ServiceInfo;
import com.example.swagger_center.dto.RegisterSpecRequest;
import com.example.swagger_center.parser.OpenApiParser;
import com.example.swagger_center.store.InMemorySpecStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SwaggerCenterServiceTest {

    private final SwaggerCenterService service =
            new SwaggerCenterService(new InMemorySpecStore(), new OpenApiParser());

    @BeforeEach
    void registerFixture() {
        service.registerSpec(new RegisterSpecRequest("payment", Fixtures.read("spec-3.0.json")));
    }

    @Test
    void rejectsNegativePage() {
        assertThatThrownBy(() -> service.getApiList("payment", null, -1, 20))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("page");
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -5, 101})
    void rejectsSizeOutOfRange(int size) {
        assertThatThrownBy(() -> service.getApiList("payment", null, 0, size))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("size");
    }

    @Test
    void rejectsRegistrationWithoutServiceName() {
        assertThatThrownBy(() -> service.registerSpec(new RegisterSpecRequest(null, Fixtures.read("spec-3.0.json"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("serviceName");
    }

    @Test
    void keywordMatchesEveryTermAcrossSummaryPathAndTags() {
        assertThat(operationIds("결제 삭제")).containsExactly("deletePayment");
        assertThat(operationIds("ADMIN")).containsExactly("deletePayment");
        assertThat(operationIds("payments")).hasSize(3);
        assertThat(operationIds("결제 없는단어")).isEmpty();
    }

    @Test
    void keywordFilterAppliesBeforePaging() {
        assertThat(service.getApiList("payment", "admin", 0, 20).totalElements()).isEqualTo(1);
    }

    @Test
    void blankKeywordReturnsEverything() {
        assertThat(operationIds(" ")).hasSize(3);
        assertThat(operationIds(null)).hasSize(3);
    }

    @Test
    void serviceInfoExposesTitleDescriptionAndServers() {
        ServiceInfo info = service.listServices().get(0);

        assertThat(info.title()).isEqualTo("Payment API");
        assertThat(info.description()).isEqualTo("결제 도메인 서비스");
        assertThat(info.servers()).containsExactly("https://pay.dev.example.com");
    }

    private List<String> operationIds(String keyword) {
        return service.getApiList("payment", keyword, 0, 20).content().stream()
                .map(ApiSummary::operationId)
                .toList();
    }
}
