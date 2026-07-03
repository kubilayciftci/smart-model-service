package com.togg.trux.smartmodel.adapter.out.persistence;

import com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.FilterBinding;
import com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.QueryParts;
import com.togg.trux.smartmodel.domain.FeatureSearchCriteria;
import com.togg.trux.smartmodel.domain.FeatureSearchResult;
import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartFeatureReadPort;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.MatchStrategy.CASE_INSENSITIVE_CONTAINS;
import static com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.MatchStrategy.EXACT;

@ApplicationScoped
public class PanacheSmartFeatureRepository
        implements PanacheRepositoryBase<SmartFeatureEntity, UUID>, SmartFeatureReadPort {

    @Override
    public Optional<SmartFeatureEntity> findFeatureById(UUID id) {
        return find("from SmartFeatureEntity f join fetch f.model where f.id = ?1", id)
                .firstResultOptional();
    }

    @Override
    public FeatureSearchResult search(FeatureSearchCriteria criteria) {
        QueryParts parts = SearchQueryBuilder.build(List.of(
                new FilterBinding("name", CASE_INSENSITIVE_CONTAINS, criteria.name()),
                new FilterBinding("identifier", EXACT, criteria.identifier()),
                new FilterBinding("type", EXACT, criteria.type()),
                new FilterBinding("category", EXACT, criteria.category()),
                new FilterBinding("model.id", EXACT, criteria.modelId())));
        PanacheQuery<SmartFeatureEntity> query =
                find(parts.whereClause(), Sort.by("name").and("id"), parts.parameters());
        long totalCount = query.count();
        List<SmartFeatureEntity> features = query
                .page(Page.of(criteria.page().zeroBasedIndex(), criteria.page().pageSize()))
                .list();
        return new FeatureSearchResult(features, totalCount);
    }
}
