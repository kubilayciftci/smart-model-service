package com.togg.trux.smartmodel.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.togg.trux.smartmodel.domain.ValidationFailedException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DescriptorValidatorTest {

    DescriptorValidator validator = new DescriptorValidator(new ObjectMapper());

    @ParameterizedTest
    @CsvSource(value = {
            "null, device, wearable, name",
            "'', device, wearable, name",
            "Watch, null, wearable, type",
            "Watch, '   ', wearable, type",
            "Watch, device, null, category",
            "Watch, device, '', category"
    }, nullValues = "null")
    void rejectsBlankMandatoryFields(String name, String type, String category, String expectedField) {
        ValidationFailedException exception = assertThrows(ValidationFailedException.class,
                () -> validator.validateDescriptor(name, type, category, null));
        assertTrue(exception.getMessage().contains(expectedField));
    }

    @ParameterizedTest
    @ValueSource(strings = {"not json", "[1,2,3]", "\"text\"", "{broken"})
    void rejectsAttributesThatAreNotJsonObjects(String attributesJson) {
        assertThrows(ValidationFailedException.class,
                () -> validator.validateDescriptor("Watch", "device", "wearable", attributesJson));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"vendor\":\"acme\",\"nested\":{\"a\":1}}"})
    void acceptsValidJsonObjects(String attributesJson) {
        assertDoesNotThrow(() -> validator.validateDescriptor("Watch", "device", "wearable", attributesJson));
    }

    @Test
    void acceptsNullAndBlankAttributes() {
        assertDoesNotThrow(() -> validator.validateDescriptor("Watch", "device", "wearable", null));
        assertDoesNotThrow(() -> validator.validateDescriptor("Watch", "device", "wearable", "  "));
    }

    @Test
    void rejectsBlankIdentifier() {
        assertThrows(ValidationFailedException.class, () -> validator.validateIdentifier(" "));
        assertDoesNotThrow(() -> validator.validateIdentifier("smart-watch"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"identifier", "name", "type", "category"})
    void rejectsFieldsExceedingMaximumLength(String fieldName) {
        ValidationFailedException exception = assertThrows(ValidationFailedException.class,
                () -> validateFieldWithValue(fieldName, "x".repeat(256)));
        assertTrue(exception.getMessage().contains(fieldName + " must be at most 255 characters"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"identifier", "name", "type", "category"})
    void acceptsFieldsAtMaximumLength(String fieldName) {
        assertDoesNotThrow(() -> validateFieldWithValue(fieldName, "x".repeat(255)));
    }

    private void validateFieldWithValue(String fieldName, String value) {
        if (fieldName.equals("identifier")) {
            validator.validateIdentifier(value);
            return;
        }
        validator.validateDescriptor(
                fieldName.equals("name") ? value : "Watch",
                fieldName.equals("type") ? value : "device",
                fieldName.equals("category") ? value : "wearable",
                null);
    }
}
