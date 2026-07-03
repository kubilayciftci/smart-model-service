package com.togg.trux.smartmodel.infrastructure;

import com.togg.trux.smartmodel.service.CatalogMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class MicrometerCatalogMetrics implements CatalogMetrics {

    private final Counter modelsCreated;
    private final Counter modelsDeleted;
    private final Counter featuresCreated;
    private final Counter modelSearches;
    private final Counter featureSearches;

    public MicrometerCatalogMetrics(MeterRegistry registry) {
        modelsCreated = registry.counter("smartmodel_created_total");
        modelsDeleted = registry.counter("smartmodel_deleted_total");
        featuresCreated = registry.counter("smartfeature_created_total");
        modelSearches = registry.counter("smartmodel_search_total");
        featureSearches = registry.counter("smartfeature_search_total");
    }

    @Override
    public void modelCreated() {
        modelsCreated.increment();
    }

    @Override
    public void modelDeleted() {
        modelsDeleted.increment();
    }

    @Override
    public void featureCreated() {
        featuresCreated.increment();
    }

    @Override
    public void modelSearchExecuted() {
        modelSearches.increment();
    }

    @Override
    public void featureSearchExecuted() {
        featureSearches.increment();
    }
}
