package com.togg.trux.smartmodel.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.togg.trux.smartmodel.domain.ValidationFailedException;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DescriptorValidator {

    private static final int MAX_FIELD_LENGTH = 255;

    private final ObjectMapper objectMapper;

    public DescriptorValidator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void validateIdentifier(String identifier) {
        requireNonBlank(identifier, "identifier");
        requireMaxLength(identifier, "identifier");
    }

    public void validateDescriptor(String name, String type, String category, String attributesJson) {
        requireNonBlank(name, "name");
        requireMaxLength(name, "name");
        requireNonBlank(type, "type");
        requireMaxLength(type, "type");
        requireNonBlank(category, "category");
        requireMaxLength(category, "category");
        validateAttributesJson(attributesJson);
    }

    private void validateAttributesJson(String attributesJson) {
        if (attributesJson == null || attributesJson.isBlank()) {
            return;
        }
        try {
            JsonNode parsed = objectMapper.readTree(attributesJson);
            if (!parsed.isObject()) {
                throw new ValidationFailedException("attributes_json must be a JSON object");
            }
        } catch (JsonProcessingException e) {
            throw new ValidationFailedException("attributes_json is not valid JSON: " + e.getOriginalMessage());
        }
    }

    private void requireNonBlank(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationFailedException(fieldName + " must not be blank");
        }
    }

    private void requireMaxLength(String value, String fieldName) {
        if (value.length() > MAX_FIELD_LENGTH) {
            throw new ValidationFailedException(fieldName + " must be at most " + MAX_FIELD_LENGTH + " characters");
        }
    }
}
