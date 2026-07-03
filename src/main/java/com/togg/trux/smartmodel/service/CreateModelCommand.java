package com.togg.trux.smartmodel.service;

import java.util.List;

public record CreateModelCommand(String identifier, String name, String type, String category,
        String attributesJson, List<FeatureInputCommand> features) {
}
