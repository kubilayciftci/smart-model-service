package com.togg.trux.smartmodel.adapter.in.grpc;

import com.togg.trux.smartmodel.domain.PageRequest;
import com.togg.trux.smartmodel.domain.ValidationFailedException;
import com.togg.trux.smartmodel.grpc.CreateModelRequest;
import com.togg.trux.smartmodel.grpc.FeatureInput;
import com.togg.trux.smartmodel.grpc.SearchModelsRequest;
import com.togg.trux.smartmodel.service.CreateModelCommand;
import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SmartModelProtoMapperTest {

    SmartModelProtoMapper mapper = new SmartModelProtoMapper();

    @Test
    void mapsCreateRequestWithFeaturesAndNormalizesEmptyAttributes() {
        CreateModelRequest request = CreateModelRequest.newBuilder()
                .setIdentifier("smart-watch")
                .setName("Smart Watch")
                .setType("device")
                .setCategory("wearable")
                .setAttributesJson("")
                .addFeatures(FeatureInput.newBuilder()
                        .setIdentifier("get-calories")
                        .setName("Get Calories")
                        .setType("telemetry")
                        .setCategory("health")
                        .setAttributesJson("{\"unit\":\"kcal\"}"))
                .build();
        CreateModelCommand command = mapper.toCreateCommand(request);
        assertEquals("smart-watch", command.identifier());
        assertNull(command.attributesJson());
        assertEquals(1, command.features().size());
        assertEquals("{\"unit\":\"kcal\"}", command.features().get(0).attributesJson());
    }

    @Test
    void mapsSearchRequestPresenceToNullableCriteria() {
        SearchModelsRequest request = SearchModelsRequest.newBuilder()
                .setType("device")
                .setPage(2)
                .setPageSize(5)
                .build();
        ModelSearchCriteria criteria = mapper.toModelCriteria(request);
        assertNull(criteria.name());
        assertNull(criteria.identifier());
        assertEquals("device", criteria.type());
        assertNull(criteria.category());
        assertEquals(PageRequest.of(2, 5), criteria.page());
    }

    @Test
    void rejectsMalformedUuid() {
        assertThrows(ValidationFailedException.class, () -> mapper.parseUuid("not-a-uuid", "id"));
    }
}
