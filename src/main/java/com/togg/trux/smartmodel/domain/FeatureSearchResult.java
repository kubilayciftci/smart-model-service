package com.togg.trux.smartmodel.domain;

import java.util.List;

public record FeatureSearchResult(List<SmartFeatureEntity> items, long totalCount) {}
