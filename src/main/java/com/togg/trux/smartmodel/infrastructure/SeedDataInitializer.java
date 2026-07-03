package com.togg.trux.smartmodel.infrastructure;

import com.togg.trux.smartmodel.domain.SmartFeatureEntity;
import com.togg.trux.smartmodel.domain.SmartModelEntity;
import com.togg.trux.smartmodel.domain.SmartModelReadPort;
import com.togg.trux.smartmodel.domain.SmartModelWritePort;
import io.quarkus.logging.Log;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.transaction.Transactional;
import java.util.List;

@ApplicationScoped
public class SeedDataInitializer {

    private final SmartModelReadPort readPort;
    private final SmartModelWritePort writePort;

    public SeedDataInitializer(SmartModelReadPort readPort, SmartModelWritePort writePort) {
        this.readPort = readPort;
        this.writePort = writePort;
    }

    @Transactional
    public void onStart(@Observes StartupEvent event) {
        if (readPort.countModels() > 0) {
            Log.info("Seed skipped, catalog is not empty");
            return;
        }
        seedModels().forEach(writePort::persist);
        Log.info("Seed data inserted: 4 smart models with features");
    }

    private List<SmartModelEntity> seedModels() {
        SmartModelEntity openWeatherMap = model("openweathermap", "OpenWeatherMap", "service", "weather",
                "{\"baseUrl\":\"https://api.openweathermap.org\",\"authType\":\"apiKey\",\"docsUrl\":\"https://openweathermap.org/api\"}");
        openWeatherMap.addFeature(feature("get-weekly-forecast-city", "Get Weekly Forecast in a City",
                "api-call", "forecast", "{\"path\":\"/data/2.5/forecast\",\"unit\":\"metric\"}"));
        openWeatherMap.addFeature(feature("get-daily-forecast-state", "Get Daily Forecast for a State",
                "api-call", "forecast", "{\"path\":\"/data/2.5/forecast/daily\",\"granularity\":\"state\"}"));

        SmartModelEntity imdb = model("imdb-movie-database", "IMDB Movie Database", "service", "entertainment",
                "{\"baseUrl\":\"https://imdb-api.example.com\",\"authType\":\"apiKey\"}");
        imdb.addFeature(feature("search-movie", "Search Movie", "api-call", "search",
                "{\"path\":\"/search/title\",\"maxResults\":25}"));

        SmartModelEntity smartWatch = model("smart-watch", "Smart Watch", "device", "wearable",
                "{\"manufacturer\":\"Acme\",\"firmwareVersion\":\"2.4.1\",\"connectivity\":\"ble\"}");
        smartWatch.addFeature(feature("get-daily-calories-burned", "Get Calories Burned for a Day",
                "telemetry", "health", "{\"unit\":\"kcal\",\"samplingRate\":\"1m\"}"));

        SmartModelEntity camera = model("remote-camera", "Remotely Controllable Camera", "device", "surveillance",
                "{\"manufacturer\":\"Acme\",\"resolution\":\"1080p\",\"protocol\":\"rtsp\"}");
        camera.addFeature(feature("take-camera-screenshot", "Take Camera Screenshot", "command", "imaging",
                "{\"format\":\"jpeg\",\"maxWidth\":1920}"));
        camera.addFeature(feature("get-camera-live-video-url", "Get Camera Live Video URL", "query", "streaming",
                "{\"protocol\":\"rtsp\",\"ttlSeconds\":300}"));

        return List.of(openWeatherMap, imdb, smartWatch, camera);
    }

    private SmartModelEntity model(String identifier, String name, String type, String category, String attributes) {
        SmartModelEntity entity = new SmartModelEntity();
        entity.setIdentifier(identifier);
        entity.setName(name);
        entity.setType(type);
        entity.setCategory(category);
        entity.setAttributes(attributes);
        return entity;
    }

    private SmartFeatureEntity feature(String identifier, String name, String type, String category,
            String attributes) {
        SmartFeatureEntity entity = new SmartFeatureEntity();
        entity.setIdentifier(identifier);
        entity.setName(name);
        entity.setType(type);
        entity.setCategory(category);
        entity.setAttributes(attributes);
        return entity;
    }
}
