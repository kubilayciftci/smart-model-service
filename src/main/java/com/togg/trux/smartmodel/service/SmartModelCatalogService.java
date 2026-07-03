package com.togg.trux.smartmodel.service;

import com.togg.trux.smartmodel.domain.DuplicateIdentifierException;
import com.togg.trux.smartmodel.domain.ModelNotFoundException;
import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchResult;
import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import com.togg.trux.smartmodel.domain.SmartModelReadPort;
import com.togg.trux.smartmodel.domain.SmartModelWritePort;
import com.togg.trux.smartmodel.domain.ValidationFailedException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class SmartModelCatalogService {

    private final SmartModelReadPort readPort;
    private final SmartModelWritePort writePort;
    private final DescriptorValidator validator;
    private final CatalogMetrics metrics;

    public SmartModelCatalogService(SmartModelReadPort readPort, SmartModelWritePort writePort,
            DescriptorValidator validator, CatalogMetrics metrics) {
        this.readPort = readPort;
        this.writePort = writePort;
        this.validator = validator;
        this.metrics = metrics;
    }

    @Transactional
    public SmartModelEntity create(CreateModelCommand command) {
        validator.validateIdentifier(command.identifier());
        validator.validateDescriptor(command.name(), command.type(), command.category(), command.attributesJson());
        command.features().forEach(this::validateFeatureInput);
        ensureFeatureIdentifiersUnique(command);
        if (readPort.existsByIdentifier(command.identifier())) {
            throw new DuplicateIdentifierException(command.identifier());
        }
        SmartModelEntity model = new SmartModelEntity();
        model.setIdentifier(command.identifier());
        applyDescriptor(model, command.name(), command.type(), command.category(), command.attributesJson());
        command.features().forEach(featureInput -> model.addFeature(toFeatureEntity(featureInput)));
        writePort.persist(model);
        metrics.modelCreated();
        command.features().forEach(featureInput -> metrics.featureCreated());
        return model;
    }

    public SmartModelEntity getWithFeatures(UUID id) {
        return readPort.findByIdWithFeatures(id).orElseThrow(() -> new ModelNotFoundException(id));
    }

    @Transactional
    public SmartModelEntity update(UpdateModelCommand command) {
        validator.validateDescriptor(command.name(), command.type(), command.category(), command.attributesJson());
        SmartModelEntity model = readPort.findModelById(command.id())
                .orElseThrow(() -> new ModelNotFoundException(command.id()));
        applyDescriptor(model, command.name(), command.type(), command.category(), command.attributesJson());
        return model;
    }

    @Transactional
    public void delete(UUID id) {
        SmartModelEntity model = readPort.findModelById(id).orElseThrow(() -> new ModelNotFoundException(id));
        writePort.delete(model);
        metrics.modelDeleted();
    }

    public ModelSearchResult search(ModelSearchCriteria criteria) {
        metrics.modelSearchExecuted();
        return readPort.search(criteria);
    }

    private void validateFeatureInput(FeatureInputCommand featureInput) {
        validator.validateIdentifier(featureInput.identifier());
        validator.validateDescriptor(featureInput.name(), featureInput.type(), featureInput.category(),
                featureInput.attributesJson());
    }

    private void ensureFeatureIdentifiersUnique(CreateModelCommand command) {
        Set<String> seen = new HashSet<>();
        command.features().forEach(featureInput -> {
            if (!seen.add(featureInput.identifier())) {
                throw new ValidationFailedException("duplicate feature identifier: " + featureInput.identifier());
            }
        });
    }

    private void applyDescriptor(SmartModelEntity model, String name, String type, String category,
            String attributesJson) {
        model.setName(name);
        model.setType(type);
        model.setCategory(category);
        model.setAttributes(attributesJson);
    }

    private SmartFeatureEntity toFeatureEntity(FeatureInputCommand featureInput) {
        SmartFeatureEntity feature = new SmartFeatureEntity();
        feature.setIdentifier(featureInput.identifier());
        feature.setName(featureInput.name());
        feature.setType(featureInput.type());
        feature.setCategory(featureInput.category());
        feature.setAttributes(featureInput.attributesJson());
        return feature;
    }
}
