package com.togg.trux.smartmodel.domain;

public class DuplicateIdentifierException extends RuntimeException {
    public DuplicateIdentifierException(String identifier) {
        super("Identifier already exists: " + identifier);
    }
}
