package com.togg.trux.smartmodel.adapter.out.persistence;

import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@QuarkusTest
class SmartModelPersistenceTest {

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void persistingModelCascadesFeaturesAndAssignsIdsAndTimestamps() {
        SmartModelEntity model = newModel("persist-cascade-" + UUID.randomUUID());
        SmartFeatureEntity feature = newFeature("feature-one");
        model.addFeature(feature);
        entityManager.persist(model);
        entityManager.flush();
        assertNotNull(model.getId());
        assertNotNull(feature.getId());
        assertNotNull(model.getCreatedAt());
        assertNotNull(feature.getUpdatedAt());
        assertEquals(model.getId(), feature.getModel().getId());
    }

    @Test
    @Transactional
    void removingFeatureFromCollectionDeletesRowViaOrphanRemoval() {
        SmartModelEntity model = newModel("orphan-removal-" + UUID.randomUUID());
        SmartFeatureEntity feature = newFeature("feature-to-remove");
        model.addFeature(feature);
        entityManager.persist(model);
        entityManager.flush();
        UUID featureId = feature.getId();
        model.removeFeature(feature);
        entityManager.flush();
        Long remaining = entityManager
                .createQuery("select count(f) from SmartFeatureEntity f where f.id = :id", Long.class)
                .setParameter("id", featureId)
                .getSingleResult();
        assertEquals(0L, remaining);
    }

    private SmartModelEntity newModel(String identifier) {
        SmartModelEntity model = new SmartModelEntity();
        model.setIdentifier(identifier);
        model.setName("Persistence Test Model");
        model.setType("device");
        model.setCategory("test");
        model.setAttributes("{\"vendor\":\"acme\"}");
        return model;
    }

    private SmartFeatureEntity newFeature(String identifier) {
        SmartFeatureEntity feature = new SmartFeatureEntity();
        feature.setIdentifier(identifier);
        feature.setName("Persistence Test Feature");
        feature.setType("command");
        feature.setCategory("test");
        return feature;
    }
}
