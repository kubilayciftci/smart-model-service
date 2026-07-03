package com.togg.trux.smartmodel.adapter.in.grpc;

import com.togg.trux.smartmodel.grpc.AddFeatureRequest;
import com.togg.trux.smartmodel.grpc.CreateModelRequest;
import com.togg.trux.smartmodel.grpc.DeleteModelRequest;
import com.togg.trux.smartmodel.grpc.FeatureInput;
import com.togg.trux.smartmodel.grpc.GetModelRequest;
import com.togg.trux.smartmodel.grpc.RemoveFeatureRequest;
import com.togg.trux.smartmodel.grpc.SearchFeaturesRequest;
import com.togg.trux.smartmodel.grpc.SearchFeaturesResponse;
import com.togg.trux.smartmodel.grpc.SearchModelsRequest;
import com.togg.trux.smartmodel.grpc.SearchModelsResponse;
import com.togg.trux.smartmodel.grpc.SmartFeature;
import com.togg.trux.smartmodel.grpc.SmartModel;
import com.togg.trux.smartmodel.grpc.SmartModelServiceGrpc;
import com.togg.trux.smartmodel.grpc.UpdateModelRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.quarkus.grpc.GrpcClient;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SmartModelGrpcServiceTest {

    @GrpcClient
    SmartModelServiceGrpc.SmartModelServiceBlockingStub smartmodel;

    @Inject
    EntityManager entityManager;

    @Test
    void createGetUpdateDeleteLifecycle() {
        String identifier = uniqueIdentifier();
        SmartModel created = smartmodel.createModel(createRequest(identifier).build());
        assertFalse(created.getId().isEmpty());
        assertEquals(identifier, created.getIdentifier());
        assertTrue(created.getCreatedAt().getSeconds() > 0);
        assertEquals(1, created.getFeaturesCount());

        SmartModel fetched = smartmodel.getModel(GetModelRequest.newBuilder().setId(created.getId()).build());
        assertEquals(created.getId(), fetched.getId());
        assertEquals(1, fetched.getFeaturesCount());

        SmartModel updated = smartmodel.updateModel(UpdateModelRequest.newBuilder()
                .setId(created.getId())
                .setName("Renamed Model")
                .setType("service")
                .setCategory("updated-category")
                .setAttributesJson("{\"version\":2}")
                .build());
        assertEquals("Renamed Model", updated.getName());
        assertEquals(identifier, updated.getIdentifier());
        assertNotEquals(created.getUpdatedAt(), updated.getUpdatedAt());

        smartmodel.deleteModel(DeleteModelRequest.newBuilder().setId(created.getId()).build());
        StatusRuntimeException notFound = assertThrows(StatusRuntimeException.class,
                () -> smartmodel.getModel(GetModelRequest.newBuilder().setId(created.getId()).build()));
        assertEquals(Status.Code.NOT_FOUND, notFound.getStatus().getCode());
    }

    @Test
    void duplicateIdentifierReturnsAlreadyExists() {
        String identifier = uniqueIdentifier();
        smartmodel.createModel(createRequest(identifier).build());
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> smartmodel.createModel(createRequest(identifier).build()));
        assertEquals(Status.Code.ALREADY_EXISTS, exception.getStatus().getCode());
    }

    @Test
    void blankNameReturnsInvalidArgument() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> smartmodel.createModel(createRequest(uniqueIdentifier()).setName("").build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    @Test
    void malformedAttributesJsonReturnsInvalidArgument() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> smartmodel.createModel(createRequest(uniqueIdentifier()).setAttributesJson("{oops").build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    @Test
    void malformedUuidReturnsInvalidArgument() {
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> smartmodel.getModel(GetModelRequest.newBuilder().setId("not-a-uuid").build()));
        assertEquals(Status.Code.INVALID_ARGUMENT, exception.getStatus().getCode());
    }

    @Test
    void searchModelsFiltersAndReportsFeatureCountWithoutEmbeddingFeatures() {
        String identifier = uniqueIdentifier();
        smartmodel.createModel(createRequest(identifier).build());
        SearchModelsResponse response = smartmodel.searchModels(
                SearchModelsRequest.newBuilder().setIdentifier(identifier).build());
        assertEquals(1, response.getTotalCount());
        assertEquals(1, response.getModels(0).getFeatureCount());
        assertEquals(identifier, response.getModels(0).getIdentifier());
    }

    @Test
    void featureLifecycleWithOrphanRemovalProof() {
        SmartModel created = smartmodel.createModel(createRequest(uniqueIdentifier()).build());
        SmartFeature added = smartmodel.addFeature(AddFeatureRequest.newBuilder()
                .setModelId(created.getId())
                .setFeature(FeatureInput.newBuilder()
                        .setIdentifier("added-feature")
                        .setName("Added Feature")
                        .setType("command")
                        .setCategory("lifecycle-test"))
                .build());
        assertFalse(added.getId().isEmpty());

        SearchFeaturesResponse byModel = smartmodel.searchFeatures(SearchFeaturesRequest.newBuilder()
                .setModelId(created.getId())
                .build());
        assertEquals(2, byModel.getTotalCount());

        smartmodel.removeFeature(RemoveFeatureRequest.newBuilder().setFeatureId(added.getId()).build());
        Long remainingRows = entityManager
                .createQuery("select count(f) from SmartFeatureEntity f where f.id = :id", Long.class)
                .setParameter("id", UUID.fromString(added.getId()))
                .getSingleResult();
        assertEquals(0L, remainingRows);
    }

    @Test
    void duplicateFeatureIdentifierWithinModelReturnsAlreadyExists() {
        SmartModel created = smartmodel.createModel(createRequest(uniqueIdentifier()).build());
        StatusRuntimeException exception = assertThrows(StatusRuntimeException.class,
                () -> smartmodel.addFeature(AddFeatureRequest.newBuilder()
                        .setModelId(created.getId())
                        .setFeature(FeatureInput.newBuilder()
                                .setIdentifier("initial-feature")
                                .setName("Duplicate")
                                .setType("command")
                                .setCategory("lifecycle-test"))
                        .build()));
        assertEquals(Status.Code.ALREADY_EXISTS, exception.getStatus().getCode());
    }

    private CreateModelRequest.Builder createRequest(String identifier) {
        return CreateModelRequest.newBuilder()
                .setIdentifier(identifier)
                .setName("Test Model " + identifier)
                .setType("device")
                .setCategory("grpc-test")
                .setAttributesJson("{\"vendor\":\"acme\"}")
                .addFeatures(FeatureInput.newBuilder()
                        .setIdentifier("initial-feature")
                        .setName("Initial Feature")
                        .setType("telemetry")
                        .setCategory("grpc-test"));
    }

    private String uniqueIdentifier() {
        return "grpc-test-" + UUID.randomUUID();
    }
}
