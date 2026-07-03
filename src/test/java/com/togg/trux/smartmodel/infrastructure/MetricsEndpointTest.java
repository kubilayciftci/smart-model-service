package com.togg.trux.smartmodel.infrastructure;

import com.togg.trux.smartmodel.grpc.SearchModelsRequest;
import com.togg.trux.smartmodel.grpc.SmartModelServiceGrpc;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class MetricsEndpointTest {

    @GrpcClient
    SmartModelServiceGrpc.SmartModelServiceBlockingStub smartmodel;

    @Test
    void businessCountersAreExposedOnPrometheusEndpoint() throws Exception {
        smartmodel.searchModels(SearchModelsRequest.newBuilder().build());
        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:8081/q/metrics")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        assertTrue(body.contains("smartmodel_created_total"));
        assertTrue(body.contains("smartmodel_deleted_total"));
        assertTrue(body.contains("smartfeature_created_total"));
        assertTrue(body.contains("smartmodel_search_total"));
        assertTrue(body.contains("smartfeature_search_total"));
    }
}
