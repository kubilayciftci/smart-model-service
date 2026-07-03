package com.togg.trux.smartmodel.domain;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PageRequestTest {

    @ParameterizedTest
    @CsvSource({
            "0, 0, 1, 20",
            "-5, -1, 1, 20",
            "1, 20, 1, 20",
            "3, 500, 3, 100",
            "2, 100, 2, 100",
            "7, 1, 7, 1"
    })
    void sanitizesPageAndPageSize(int rawPage, int rawSize, int expectedPage, int expectedSize) {
        PageRequest request = PageRequest.of(rawPage, rawSize);
        assertEquals(expectedPage, request.page());
        assertEquals(expectedSize, request.pageSize());
        assertEquals(expectedPage - 1, request.zeroBasedIndex());
    }
}
