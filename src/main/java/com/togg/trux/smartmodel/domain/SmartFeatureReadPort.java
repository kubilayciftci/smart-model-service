package com.togg.trux.smartmodel.domain;

import java.util.Optional;
import java.util.UUID;

public interface SmartFeatureReadPort {
    Optional<SmartFeatureEntity> findFeatureById(UUID id);
    FeatureSearchResult search(FeatureSearchCriteria criteria);
}
