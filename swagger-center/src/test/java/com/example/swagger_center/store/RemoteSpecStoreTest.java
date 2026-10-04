package com.example.swagger_center.store;

import com.example.swagger_center.Fixtures;
import com.example.swagger_center.config.SwaggerCenterProperties;
import com.example.swagger_center.store.SpecStore.StoredSpec;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.ResponseCreator;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class RemoteSpecStoreTest {

    private static final String ORDER_URL = "http://ms-order/v3/api-docs";
    private static final String USER_URL = "http://ms-user/v3/api-docs";
    private static final Duration ONE_MINUTE = Duration.ofMinutes(1);

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer server = MockRestServiceServer.bindTo(builder).ignoreExpectOrder(true).build();

    @Test
    void fetchesSpecFromTheConfiguredUrl() {
        server.expect(requestTo(ORDER_URL)).andRespond(json("spec-3.1.json"));

        StoredSpec spec = store(ONE_MINUTE).findByServiceName("ms-order").orElseThrow();

        assertThat(spec.serviceName()).isEqualTo("ms-order");
        assertThat(spec.parsedSpec().getPaths()).containsKey("/api/orders");
    }

    @Test
    void unknownServiceIsEmptyWithoutFetching() {
        assertThat(store(ONE_MINUTE).findByServiceName("nope")).isEmpty();
    }

    @Test
    void servesFromCacheWithinTtl() {
        server.expect(ExpectedCount.once(), requestTo(ORDER_URL)).andRespond(json("spec-3.1.json"));
        RemoteSpecStore store = store(ONE_MINUTE);

        store.findByServiceName("ms-order");
        store.findByServiceName("ms-order");

        server.verify();
    }

    @Test
    void refetchesAfterTtlSoSpecChangesShowUp() {
        server.expect(ExpectedCount.once(), requestTo(ORDER_URL)).andRespond(json("spec-3.1.json"));
        server.expect(ExpectedCount.once(), requestTo(ORDER_URL)).andRespond(json("spec-3.0.json"));
        RemoteSpecStore store = store(Duration.ZERO);

        assertThat(store.findByServiceName("ms-order").orElseThrow().parsedSpec().getPaths())
                .containsKey("/api/orders");
        assertThat(store.findByServiceName("ms-order").orElseThrow().parsedSpec().getPaths())
                .containsKey("/v1/payments");
    }

    @Test
    void findAllSkipsServicesThatCannotBeFetched() {
        server.expect(requestTo(ORDER_URL)).andRespond(json("spec-3.1.json"));
        server.expect(requestTo(USER_URL)).andRespond(withServerError());

        assertThat(store(ONE_MINUTE).findAll()).extracting(StoredSpec::serviceName).containsExactly("ms-order");
    }

    @Test
    void failedFetchNamesTheServiceAndUrl() {
        server.expect(requestTo(USER_URL)).andRespond(withServerError());

        assertThatThrownBy(() -> store(ONE_MINUTE).findByServiceName("ms-user"))
                .isInstanceOf(SpecFetchException.class)
                .hasMessageContaining("ms-user")
                .hasMessageContaining(USER_URL);
    }

    @Test
    void unparsableSpecIsReportedAsFetchFailure() {
        server.expect(requestTo(USER_URL)).andRespond(withSuccess("not an openapi document", MediaType.TEXT_PLAIN));

        assertThatThrownBy(() -> store(ONE_MINUTE).findByServiceName("ms-user"))
                .isInstanceOf(SpecFetchException.class)
                .hasMessageContaining("ms-user");
    }

    private RemoteSpecStore store(Duration cacheTtl) {
        Map<String, String> services = new LinkedHashMap<>();
        services.put("ms-order", ORDER_URL);
        services.put("ms-user", USER_URL);
        return new RemoteSpecStore(new SwaggerCenterProperties(services, cacheTtl), builder.build());
    }

    private static ResponseCreator json(String fixture) {
        return withSuccess(Fixtures.read(fixture), MediaType.APPLICATION_JSON);
    }
}
