package com.togg.trux.smartmodel.adapter.in.grpc;

import com.togg.trux.smartmodel.domain.FeatureSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import com.togg.trux.smartmodel.grpc.AddFeatureRequest;
import com.togg.trux.smartmodel.grpc.CreateModelRequest;
import com.togg.trux.smartmodel.grpc.DeleteModelRequest;
import com.togg.trux.smartmodel.grpc.DeleteModelResponse;
import com.togg.trux.smartmodel.grpc.GetModelRequest;
import com.togg.trux.smartmodel.grpc.RemoveFeatureRequest;
import com.togg.trux.smartmodel.grpc.RemoveFeatureResponse;
import com.togg.trux.smartmodel.grpc.SearchFeaturesRequest;
import com.togg.trux.smartmodel.grpc.SearchFeaturesResponse;
import com.togg.trux.smartmodel.grpc.SearchModelsRequest;
import com.togg.trux.smartmodel.grpc.SearchModelsResponse;
import com.togg.trux.smartmodel.grpc.SmartFeature;
import com.togg.trux.smartmodel.grpc.SmartModel;
import com.togg.trux.smartmodel.grpc.SmartModelService;
import com.togg.trux.smartmodel.grpc.UpdateFeatureRequest;
import com.togg.trux.smartmodel.grpc.UpdateModelRequest;
import com.togg.trux.smartmodel.service.SmartFeatureCatalogService;
import com.togg.trux.smartmodel.service.SmartModelCatalogService;
import com.togg.trux.smartmodel.service.UpdateModelCommand;
import io.quarkus.grpc.GrpcService;
import io.smallrye.common.annotation.RunOnVirtualThread;
import io.smallrye.mutiny.Uni;

@GrpcService
@RunOnVirtualThread
public class SmartModelGrpcService implements SmartModelService {

    private final SmartModelCatalogService modelService;
    private final SmartFeatureCatalogService featureService;
    private final SmartModelProtoMapper mapper;

    public SmartModelGrpcService(SmartModelCatalogService modelService,
            SmartFeatureCatalogService featureService, SmartModelProtoMapper mapper) {
        this.modelService = modelService;
        this.featureService = featureService;
        this.mapper = mapper;
    }

    @Override
    public Uni<SmartModel> createModel(CreateModelRequest request) {
        return Uni.createFrom().item(mapper.toProto(modelService.create(mapper.toCreateCommand(request))));
    }

    @Override
    public Uni<SmartModel> getModel(GetModelRequest request) {
        return Uni.createFrom().item(
                mapper.toProto(modelService.getWithFeatures(mapper.parseUuid(request.getId(), "id"))));
    }

    @Override
    public Uni<SmartModel> updateModel(UpdateModelRequest request) {
        UpdateModelCommand command = mapper.toUpdateCommand(request);
        modelService.update(command);
        SmartModel updated = mapper.toProto(modelService.getWithFeatures(command.id()));
        return Uni.createFrom().item(updated);
    }

    @Override
    public Uni<DeleteModelResponse> deleteModel(DeleteModelRequest request) {
        modelService.delete(mapper.parseUuid(request.getId(), "id"));
        return Uni.createFrom().item(DeleteModelResponse.getDefaultInstance());
    }

    @Override
    public Uni<SearchModelsResponse> searchModels(SearchModelsRequest request) {
        ModelSearchCriteria criteria = mapper.toModelCriteria(request);
        return Uni.createFrom().item(
                mapper.toSearchModelsResponse(modelService.search(criteria), criteria.page()));
    }

    @Override
    public Uni<SmartFeature> addFeature(AddFeatureRequest request) {
        SmartFeature added = mapper.toProto(featureService.addFeature(
                mapper.parseUuid(request.getModelId(), "model_id"),
                mapper.toFeatureCommand(request.getFeature())));
        return Uni.createFrom().item(added);
    }

    @Override
    public Uni<SmartFeature> updateFeature(UpdateFeatureRequest request) {
        SmartFeature updated = mapper.toProto(
                featureService.updateFeature(mapper.toUpdateFeatureCommand(request)));
        return Uni.createFrom().item(updated);
    }

    @Override
    public Uni<RemoveFeatureResponse> removeFeature(RemoveFeatureRequest request) {
        featureService.removeFeature(mapper.parseUuid(request.getFeatureId(), "feature_id"));
        return Uni.createFrom().item(RemoveFeatureResponse.getDefaultInstance());
    }

    @Override
    public Uni<SearchFeaturesResponse> searchFeatures(SearchFeaturesRequest request) {
        FeatureSearchCriteria criteria = mapper.toFeatureCriteria(request);
        return Uni.createFrom().item(
                mapper.toSearchFeaturesResponse(featureService.search(criteria), criteria.page()));
    }
}
