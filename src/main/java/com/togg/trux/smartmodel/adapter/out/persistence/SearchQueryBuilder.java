package com.togg.trux.smartmodel.adapter.out.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class SearchQueryBuilder {

    public enum MatchStrategy { EXACT, CASE_INSENSITIVE_CONTAINS }

    public record FilterBinding(String field, MatchStrategy strategy, Object value) {}

    public record QueryParts(String whereClause, Map<String, Object> parameters) {}

    private SearchQueryBuilder() {}

    public static QueryParts build(List<FilterBinding> bindings) {
        List<String> clauses = new ArrayList<>();
        Map<String, Object> parameters = new LinkedHashMap<>();
        for (FilterBinding binding : bindings) {
            if (isAbsent(binding.value())) {
                continue;
            }
            String parameterName = binding.field().replace('.', '_');
            clauses.add(toClause(binding, parameterName));
            parameters.put(parameterName, toParameterValue(binding));
        }
        String whereClause = clauses.isEmpty() ? "1=1" : String.join(" and ", clauses);
        return new QueryParts(whereClause, parameters);
    }

    private static boolean isAbsent(Object value) {
        return value == null || (value instanceof String text && text.isBlank());
    }

    private static String toClause(FilterBinding binding, String parameterName) {
        return switch (binding.strategy()) {
            case EXACT -> binding.field() + " = :" + parameterName;
            case CASE_INSENSITIVE_CONTAINS -> "lower(" + binding.field() + ") like :" + parameterName;
        };
    }

    private static Object toParameterValue(FilterBinding binding) {
        return switch (binding.strategy()) {
            case EXACT -> binding.value();
            case CASE_INSENSITIVE_CONTAINS -> "%" + binding.value().toString().toLowerCase(Locale.ROOT) + "%";
        };
    }
}
