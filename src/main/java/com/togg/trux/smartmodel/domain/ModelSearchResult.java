package com.togg.trux.smartmodel.domain;

import java.util.List;

public record ModelSearchResult(List<ModelWithFeatureCount> items, long totalCount) {}
