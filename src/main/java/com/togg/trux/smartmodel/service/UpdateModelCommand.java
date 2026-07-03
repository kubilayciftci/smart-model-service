package com.togg.trux.smartmodel.service;

import java.util.UUID;

public record UpdateModelCommand(UUID id, String name, String type, String category,
        String attributesJson) {
}
