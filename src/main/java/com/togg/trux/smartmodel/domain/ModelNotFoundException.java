package com.togg.trux.smartmodel.domain;

import java.util.UUID;

public class ModelNotFoundException extends RuntimeException {
    public ModelNotFoundException(UUID id) {
        super("Smart model not found: " + id);
    }
}
