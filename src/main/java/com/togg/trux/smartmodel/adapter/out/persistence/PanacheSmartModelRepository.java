package com.togg.trux.smartmodel.adapter.out.persistence;

import com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.FilterBinding;
import com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.QueryParts;
import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchResult;
import com.togg.trux.smartmodel.domain.ModelWithFeatureCount;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import com.togg.trux.smartmodel.domain.SmartModelReadPort;
import com.togg.trux.smartmodel.domain.SmartModelWritePort;
import io.quarkus.hibernate.orm.panache.PanacheQuery;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.MatchStrategy.CASE_INSENSITIVE_CONTAINS;
import static com.togg.trux.smartmodel.adapter.out.persistence.SearchQueryBuilder.MatchStrategy.EXACT;

@ApplicationScoped
public class PanacheSmartModelRepository
        implements PanacheRepositoryBase<SmartModelEntity, UUID>, SmartModelReadPort, SmartModelWritePort {

    @Override
    public void persist(SmartModelEntity model) {
        PanacheRepositoryBase.super.persist(model);
    }

    @Override
    public void delete(SmartModelEntity model) {
        PanacheRepositoryBase.super.delete(model);
    }

    @Override
    public Optional<SmartModelEntity> findModelById(UUID id) {
        return findByIdOptional(id);
    }

    @Override
    public Optional<SmartModelEntity> findByIdWithFeatures(UUID id) {
        return find("from SmartModelEntity m left join fetch m.features where m.id = ?1", id)
                .list()
                .stream()
                .findFirst();
    }

    @Override
    public boolean existsByIdentifier(String identifier) {
        return count("identifier", identifier) > 0;
    }

    @Override
    public long countModels() {
        return count();
    }

    @Override
    public ModelSearchResult search(ModelSearchCriteria criteria) {
        QueryParts parts = SearchQueryBuilder.build(List.of(
                new FilterBinding("name", CASE_INSENSITIVE_CONTAINS, criteria.name()),
                new FilterBinding("identifier", EXACT, criteria.identifier()),
                new FilterBinding("type", EXACT, criteria.type()),
                new FilterBinding("category", EXACT, criteria.category())));
        PanacheQuery<SmartModelEntity> query =
                find(parts.whereClause(), Sort.by("name").and("id"), parts.parameters());
        long totalCount = query.count();
        List<SmartModelEntity> models = query
                .page(Page.of(criteria.page().zeroBasedIndex(), criteria.page().pageSize()))
                .list();
        Map<UUID, Long> featureCounts = featureCountsByModelId(models);
        List<ModelWithFeatureCount> items = models.stream()
                .map(model -> new ModelWithFeatureCount(model, featureCounts.getOrDefault(model.getId(), 0L)))
                .toList();
        return new ModelSearchResult(items, totalCount);
    }

    private Map<UUID, Long> featureCountsByModelId(List<SmartModelEntity> models) {
        if (models.isEmpty()) {
            return Map.of();
        }
        List<UUID> modelIds = models.stream().map(SmartModelEntity::getId).toList();
        return getEntityManager()
                .createQuery(
                        "select f.model.id, count(f) from SmartFeatureEntity f where f.model.id in :modelIds group by f.model.id",
                        Object[].class)
                .setParameter("modelIds", modelIds)
                .getResultStream()
                .collect(Collectors.toMap(row -> (UUID) row[0], row -> (Long) row[1]));
    }
}
