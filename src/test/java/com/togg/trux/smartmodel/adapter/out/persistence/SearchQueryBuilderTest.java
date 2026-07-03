package com.togg.trux.smartmodel.adapter.out.persistence;

import com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.FilterBinding;
import com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.QueryParts;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.MatchStrategy.CASE_INSENSITIVE_CONTAINS;
import static com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.MatchStrategy.EXACT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchQueryBuilderTest {

    @Test
    void noFiltersProducesMatchAllClause() {
        QueryParts parts = SearchQueryBuilder.build(List.of(
                new FilterBinding("name", CASE_INSENSITIVE_CONTAINS, null),
                new FilterBinding("type", EXACT, null)));
        assertEquals("1=1", parts.whereClause());
        assertTrue(parts.parameters().isEmpty());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void blankStringValuesAreSkipped(String blank) {
        QueryParts parts = SearchQueryBuilder.build(List.of(new FilterBinding("type", EXACT, blank)));
        assertEquals("1=1", parts.whereClause());
    }

    @Test
    void containsStrategyLowercasesAndWraps() {
        QueryParts parts = SearchQueryBuilder.build(List.of(
                new FilterBinding("name", CASE_INSENSITIVE_CONTAINS, "Weather")));
        assertEquals("lower(name) like :name", parts.whereClause());
        assertEquals("%weather%", parts.parameters().get("name"));
    }

    @Test
    void multipleFiltersJoinWithAndPreservingOrder() {
        UUID modelId = UUID.randomUUID();
        QueryParts parts = SearchQueryBuilder.build(List.of(
                new FilterBinding("name", CASE_INSENSITIVE_CONTAINS, "cam"),
                new FilterBinding("type", EXACT, "device"),
                new FilterBinding("model.id", EXACT, modelId)));
        assertEquals("lower(name) like :name and type = :type and model.id = :model_id", parts.whereClause());
        assertEquals("device", parts.parameters().get("type"));
        assertEquals(modelId, parts.parameters().get("model_id"));
    }
}
