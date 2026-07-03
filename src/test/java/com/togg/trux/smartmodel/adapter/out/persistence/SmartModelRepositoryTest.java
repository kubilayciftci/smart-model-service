package com.togg.trux.smartmodel.adapter.out.persistence;

import com.togg.trux.smartmodel.domain.FeatureSearchCriteria;
import com.togg.trux.smartmodel.domain.FeatureSearchResult;
import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchResult;
import com.togg.trux.smartmodel.domain.PageRequest;
import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import com.togg.trux.smartmodel.domain.SmartModelReadPort;
import com.togg.trux.smartmodel.domain.SmartModelWritePort;
import com.togg.trux.smartmodel.domain.SmartFeatureReadPort;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SmartModelRepositoryTest {

    @Inject
    SmartModelReadPort modelReadPort;

    @Inject
    SmartModelWritePort modelWritePort;

    @Inject
    SmartFeatureReadPort featureReadPort;

    String runMarker;

    @BeforeEach
    void setUpTestData() {
        runMarker = "repo-" + UUID.randomUUID().toString().substring(0, 8);
        persistFixture();
    }

    @Transactional
    void persistFixture() {
        SmartModelEntity weather = model(runMarker + "-weather", "Weather Hub " + runMarker, "service", runMarker);
        weather.addFeature(feature(runMarker + "-forecast", "Weekly Forecast", "api-call", "forecast"));
        weather.addFeature(feature(runMarker + "-daily", "Daily Forecast", "api-call", "forecast"));
        SmartModelEntity camera = model(runMarker + "-camera", "Garden Camera " + runMarker, "device", runMarker);
        camera.addFeature(feature(runMarker + "-shot", "Take Screenshot", "command", "imaging"));
        SmartModelEntity watch = model(runMarker + "-watch", "Fitness Watch " + runMarker, "device", runMarker);
        modelWritePort.persist(weather);
        modelWritePort.persist(camera);
        modelWritePort.persist(watch);
    }

    @Test
    void searchByCategoryReturnsAllWithFeatureCountsOrderedByName() {
        ModelSearchResult result = modelReadPort.search(
                new ModelSearchCriteria(null, null, null, runMarker, PageRequest.of(1, 20)));
        assertEquals(3, result.totalCount());
        assertEquals("Fitness Watch " + runMarker, result.items().get(0).model().getName());
        assertEquals("Garden Camera " + runMarker, result.items().get(1).model().getName());
        assertEquals(1, result.items().get(1).featureCount());
        assertEquals(2, result.items().get(2).featureCount());
        assertEquals(0, result.items().get(0).featureCount());
    }

    @Test
    void searchByPartialNameIsCaseInsensitive() {
        ModelSearchResult result = modelReadPort.search(
                new ModelSearchCriteria("gArDeN", null, null, runMarker, PageRequest.of(1, 20)));
        assertEquals(1, result.totalCount());
    }

    @Test
    void searchCombinesTypeAndCategory() {
        ModelSearchResult result = modelReadPort.search(
                new ModelSearchCriteria(null, null, "device", runMarker, PageRequest.of(1, 20)));
        assertEquals(2, result.totalCount());
    }

    @Test
    void paginationLimitsItemsButReportsTotal() {
        ModelSearchResult firstPage = modelReadPort.search(
                new ModelSearchCriteria(null, null, null, runMarker, PageRequest.of(1, 2)));
        ModelSearchResult secondPage = modelReadPort.search(
                new ModelSearchCriteria(null, null, null, runMarker, PageRequest.of(2, 2)));
        assertEquals(3, firstPage.totalCount());
        assertEquals(2, firstPage.items().size());
        assertEquals(1, secondPage.items().size());
    }

    @Test
    void findByIdWithFeaturesInitializesCollection() {
        UUID weatherId = modelReadPort.search(
                        new ModelSearchCriteria(null, runMarker + "-weather", null, null, PageRequest.of(1, 1)))
                .items().get(0).model().getId();
        SmartModelEntity loaded = modelReadPort.findByIdWithFeatures(weatherId).orElseThrow();
        assertEquals(2, loaded.getFeatures().size());
    }

    @Test
    void featureSearchFiltersByCategoryAndModel() {
        UUID weatherId = modelReadPort.search(
                        new ModelSearchCriteria(null, runMarker + "-weather", null, null, PageRequest.of(1, 1)))
                .items().get(0).model().getId();
        FeatureSearchResult byModel = featureReadPort.search(
                new FeatureSearchCriteria(null, null, null, null, weatherId, PageRequest.of(1, 20)));
        FeatureSearchResult byCategory = featureReadPort.search(
                new FeatureSearchCriteria(null, null, null, "imaging", null, PageRequest.of(1, 20)));
        assertEquals(2, byModel.totalCount());
        assertTrue(byCategory.totalCount() >= 1);
    }

    @Test
    void existsByIdentifierDetectsPresence() {
        assertTrue(modelReadPort.existsByIdentifier(runMarker + "-camera"));
        assertEquals(false, modelReadPort.existsByIdentifier(runMarker + "-missing"));
    }

    private SmartModelEntity model(String identifier, String name, String type, String category) {
        SmartModelEntity entity = new SmartModelEntity();
        entity.setIdentifier(identifier);
        entity.setName(name);
        entity.setType(type);
        entity.setCategory(category);
        return entity;
    }

    private SmartFeatureEntity feature(String identifier, String name, String type, String category) {
        SmartFeatureEntity entity = new SmartFeatureEntity();
        entity.setIdentifier(identifier);
        entity.setName(name);
        entity.setType(type);
        entity.setCategory(category);
        return entity;
    }
}
