package com.togg.trux.smartmodel.domain;

public record PageRequest(int page, int pageSize) {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    public static PageRequest of(int page, int pageSize) {
        int sanitizedPage = Math.max(page, 1);
        int sanitizedSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return new PageRequest(sanitizedPage, sanitizedSize);
    }

    public int zeroBasedIndex() {
        return page - 1;
    }
}
