package com.togg.trux.smartmodel.infrastructure;

import com.togg.trux.smartmodel.domain.ModelSearchCriteria;
import com.togg.trux.smartmodel.domain.ModelSearchResult;
import com.togg.trux.smartmodel.domain.PageRequest;
import com.togg.trux.smartmodel.domain.SmartModelReadPort;
import io.quarkus.runtime.StartupEvent;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class SeedDataInitializerTest {

    @Inject
    SmartModelReadPort readPort;

    @Inject
    SeedDataInitializer initializer;

    @Test
    void seedModelsArePresentAfterStartup() {
        assertEquals(1, countByIdentifier("openweathermap"));
        assertEquals(1, countByIdentifier("imdb-movie-database"));
        assertEquals(1, countByIdentifier("smart-watch"));
        assertEquals(1, countByIdentifier("remote-camera"));
    }

    @Test
    void seedIsIdempotentWhenFiredAgain() {
        initializer.onStart(new StartupEvent());
        assertEquals(1, countByIdentifier("openweathermap"));
    }

    @Test
    void seedModelsCarryFeatures() {
        ModelSearchResult result = readPort.search(
                new ModelSearchCriteria(null, "openweathermap", null, null, PageRequest.of(1, 1)));
        assertTrue(result.items().get(0).featureCount() >= 2);
    }

    private long countByIdentifier(String identifier) {
        return readPort.search(new ModelSearchCriteria(null, identifier, null, null, PageRequest.of(1, 10)))
                .totalCount();
    }
}
