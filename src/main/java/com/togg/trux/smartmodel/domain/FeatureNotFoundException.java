package com.togg.trux.smartmodel.domain;

import java.util.UUID;

public class FeatureNotFoundException extends RuntimeException {
    public FeatureNotFoundException(UUID id) {
        super("Smart feature not found: " + id);
    }
}
