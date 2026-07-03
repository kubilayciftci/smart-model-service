package com.togg.trux.smartmodel.service;

public interface CatalogMetrics {

    void modelCreated();

    void modelDeleted();

    void featureCreated();

    void modelSearchExecuted();

    void featureSearchExecuted();
}
