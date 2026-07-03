package com.togg.trux.smartmodel.service;

public record FeatureInputCommand(String identifier, String name, String type, String category,
        String attributesJson) {
}
