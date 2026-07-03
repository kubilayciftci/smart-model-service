package com.togg.trux.smartmodel.adapter.in.grpc;

import com.google.protobuf.Timestamp;
import com.togg.trux.smartmodel.domain.FeatureSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchResult;
import com.togg.trux.smartmodel.domain.ModelWithFeatureCount;
import com.togg.trux.smartmodel.domain.FeatureSearchResult;
import com.togg.trux.smartmodel.domain.PageRequest;
import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import com.togg.trux.smartmodel.domain.ValidationFailedException;
import com.togg.trux.smartmodel.grpc.CreateModelRequest;
import com.togg.trux.smartmodel.grpc.FeatureInput;
import com.togg.trux.smartmodel.grpc.SearchFeaturesRequest;
import com.togg.trux.smartmodel.grpc.SearchFeaturesResponse;
import com.togg.trux.smartmodel.grpc.SearchModelsRequest;
import com.togg.trux.smartmodel.grpc.SearchModelsResponse;
import com.togg.trux.smartmodel.grpc.SmartFeature;
import com.togg.trux.smartmodel.grpc.SmartModel;
import com.togg.trux.smartmodel.grpc.SmartModelSummary;
import com.togg.trux.smartmodel.grpc.UpdateFeatureRequest;
import com.togg.trux.smartmodel.grpc.UpdateModelRequest;
import com.togg.trux.smartmodel.service.CreateModelCommand;
import com.togg.trux.smartmodel.service.FeatureInputCommand;
import com.togg.trux.smartmodel.service.UpdateFeatureCommand;
import com.togg.trux.smartmodel.service.UpdateModelCommand;
import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.UUID;

@ApplicationScoped
public class SmartModelProtoMapper {

    public CreateModelCommand toCreateCommand(CreateModelRequest request) {
        return new CreateModelCommand(
                request.getIdentifier(),
                request.getName(),
                request.getType(),
                request.getCategory(),
                emptyToNull(request.getAttributesJson()),
                request.getFeaturesList().stream().map(this::toFeatureCommand).toList());
    }

    public FeatureInputCommand toFeatureCommand(FeatureInput input) {
        return new FeatureInputCommand(
                input.getIdentifier(),
                input.getName(),
                input.getType(),
                input.getCategory(),
                emptyToNull(input.getAttributesJson()));
    }

    public UpdateModelCommand toUpdateCommand(UpdateModelRequest request) {
        return new UpdateModelCommand(
                parseUuid(request.getId(), "id"),
                request.getName(),
                request.getType(),
                request.getCategory(),
                emptyToNull(request.getAttributesJson()));
    }

    public UpdateFeatureCommand toUpdateFeatureCommand(UpdateFeatureRequest request) {
        return new UpdateFeatureCommand(
                parseUuid(request.getFeatureId(), "feature_id"),
                request.getName(),
                request.getType(),
                request.getCategory(),
                emptyToNull(request.getAttributesJson()));
    }

    public ModelSearchCriteria toModelCriteria(SearchModelsRequest request) {
        return new ModelSearchCriteria(
                request.hasName() ? request.getName() : null,
                request.hasIdentifier() ? request.getIdentifier() : null,
                request.hasType() ? request.getType() : null,
                request.hasCategory() ? request.getCategory() : null,
                PageRequest.of(request.getPage(), request.getPageSize()));
    }

    public FeatureSearchCriteria toFeatureCriteria(SearchFeaturesRequest request) {
        return new FeatureSearchCriteria(
                request.hasName() ? request.getName() : null,
                request.hasIdentifier() ? request.getIdentifier() : null,
                request.hasType() ? request.getType() : null,
                request.hasCategory() ? request.getCategory() : null,
                request.hasModelId() ? parseUuid(request.getModelId(), "model_id") : null,
                PageRequest.of(request.getPage(), request.getPageSize()));
    }

    public SmartModel toProto(SmartModelEntity entity) {
        SmartModel.Builder builder = SmartModel.newBuilder()
                .setId(entity.getId().toString())
                .setIdentifier(entity.getIdentifier())
                .setName(entity.getName())
                .setType(entity.getType())
                .setCategory(entity.getCategory())
                .setAttributesJson(nullToEmpty(entity.getAttributes()))
                .setCreatedAt(toTimestamp(entity.getCreatedAt()))
                .setUpdatedAt(toTimestamp(entity.getUpdatedAt()));
        entity.getFeatures().forEach(feature -> builder.addFeatures(toProto(feature)));
        return builder.build();
    }

    public SmartFeature toProto(SmartFeatureEntity entity) {
        return SmartFeature.newBuilder()
                .setId(entity.getId().toString())
                .setModelId(entity.getModel().getId().toString())
                .setIdentifier(entity.getIdentifier())
                .setName(entity.getName())
                .setType(entity.getType())
                .setCategory(entity.getCategory())
                .setAttributesJson(nullToEmpty(entity.getAttributes()))
                .setCreatedAt(toTimestamp(entity.getCreatedAt()))
                .setUpdatedAt(toTimestamp(entity.getUpdatedAt()))
                .build();
    }

    public SearchModelsResponse toSearchModelsResponse(ModelSearchResult result, PageRequest page) {
        SearchModelsResponse.Builder builder = SearchModelsResponse.newBuilder()
                .setTotalCount(result.totalCount())
                .setPage(page.page())
                .setPageSize(page.pageSize());
        result.items().forEach(item -> builder.addModels(toSummary(item)));
        return builder.build();
    }

    public SearchFeaturesResponse toSearchFeaturesResponse(FeatureSearchResult result, PageRequest page) {
        SearchFeaturesResponse.Builder builder = SearchFeaturesResponse.newBuilder()
                .setTotalCount(result.totalCount())
                .setPage(page.page())
                .setPageSize(page.pageSize());
        result.items().forEach(feature -> builder.addFeatures(toProto(feature)));
        return builder.build();
    }

    public UUID parseUuid(String rawId, String fieldName) {
        try {
            return UUID.fromString(rawId);
        } catch (IllegalArgumentException e) {
            throw new ValidationFailedException(fieldName + " must be a valid UUID: " + rawId);
        }
    }

    private SmartModelSummary toSummary(ModelWithFeatureCount item) {
        SmartModelEntity entity = item.model();
        return SmartModelSummary.newBuilder()
                .setId(entity.getId().toString())
                .setIdentifier(entity.getIdentifier())
                .setName(entity.getName())
                .setType(entity.getType())
                .setCategory(entity.getCategory())
                .setAttributesJson(nullToEmpty(entity.getAttributes()))
                .setCreatedAt(toTimestamp(entity.getCreatedAt()))
                .setUpdatedAt(toTimestamp(entity.getUpdatedAt()))
                .setFeatureCount(item.featureCount())
                .build();
    }

    private static Timestamp toTimestamp(Instant instant) {
        if (instant == null) {
            return Timestamp.getDefaultInstance();
        }
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    private static String emptyToNull(String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
