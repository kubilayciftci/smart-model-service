package com.togg.trux.smartmodel.domain;

import java.util.Optional;
import java.util.UUID;

public interface SmartModelReadPort {
    Optional<SmartModelEntity> findModelById(UUID id);
    Optional<SmartModelEntity> findByIdWithFeatures(UUID id);
    boolean existsByIdentifier(String identifier);
    long countModels();
    ModelSearchResult search(ModelSearchCriteria criteria);
}
