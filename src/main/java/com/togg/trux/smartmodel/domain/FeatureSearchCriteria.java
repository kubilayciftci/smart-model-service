package com.togg.trux.smartmodel.domain;

import java.util.UUID;

public record FeatureSearchCriteria(String name, String identifier, String type, String category, UUID modelId, PageRequest page) {}
