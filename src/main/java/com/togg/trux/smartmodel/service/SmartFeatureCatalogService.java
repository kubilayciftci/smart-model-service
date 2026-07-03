package com.togg.trux.smartmodel.service;

import com.togg.trux.smartmodel.domain.DuplicateIdentifierException;
import com.togg.trux.smartmodel.domain.FeatureNotFoundException;
import com.togg.trux.smartmodel.domain.FeatureSearchCriteria;
import com.togg.trux.smartmodel.domain.FeatureSearchResult;
import com.togg.trux.smartmodel.domain.ModelNotFoundException;
import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartFeatureReadPort;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import com.togg.trux.smartmodel.domain.SmartModelReadPort;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.UUID;

@ApplicationScoped
public class SmartFeatureCatalogService {

    private final SmartModelReadPort modelReadPort;
    private final SmartFeatureReadPort featureReadPort;
    private final DescriptorValidator validator;
    private final CatalogMetrics metrics;

    public SmartFeatureCatalogService(SmartModelReadPort modelReadPort, SmartFeatureReadPort featureReadPort,
            DescriptorValidator validator, CatalogMetrics metrics) {
        this.modelReadPort = modelReadPort;
        this.featureReadPort = featureReadPort;
        this.validator = validator;
        this.metrics = metrics;
    }

    @Transactional
    public SmartFeatureEntity addFeature(UUID modelId, FeatureInputCommand command) {
        validator.validateIdentifier(command.identifier());
        validator.validateDescriptor(command.name(), command.type(), command.category(), command.attributesJson());
        SmartModelEntity model = modelReadPort.findByIdWithFeatures(modelId)
                .orElseThrow(() -> new ModelNotFoundException(modelId));
        boolean identifierTaken = model.getFeatures().stream()
                .anyMatch(existing -> existing.getIdentifier().equals(command.identifier()));
        if (identifierTaken) {
            throw new DuplicateIdentifierException(command.identifier());
        }
        SmartFeatureEntity feature = new SmartFeatureEntity();
        feature.setIdentifier(command.identifier());
        feature.setName(command.name());
        feature.setType(command.type());
        feature.setCategory(command.category());
        feature.setAttributes(command.attributesJson());
        model.addFeature(feature);
        metrics.featureCreated();
        return feature;
    }

    @Transactional
    public SmartFeatureEntity updateFeature(UpdateFeatureCommand command) {
        validator.validateDescriptor(command.name(), command.type(), command.category(), command.attributesJson());
        SmartFeatureEntity feature = featureReadPort.findFeatureById(command.id())
                .orElseThrow(() -> new FeatureNotFoundException(command.id()));
        feature.setName(command.name());
        feature.setType(command.type());
        feature.setCategory(command.category());
        feature.setAttributes(command.attributesJson());
        return feature;
    }

    @Transactional
    public void removeFeature(UUID featureId) {
        SmartFeatureEntity feature = featureReadPort.findFeatureById(featureId)
                .orElseThrow(() -> new FeatureNotFoundException(featureId));
        feature.getModel().removeFeature(feature);
    }

    public FeatureSearchResult search(FeatureSearchCriteria criteria) {
        metrics.featureSearchExecuted();
        return featureReadPort.search(criteria);
    }
}
