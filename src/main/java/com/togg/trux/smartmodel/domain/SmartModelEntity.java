package com.togg.trux.smartmodel.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "smart_model")
public class SmartModelEntity extends CatalogDescriptorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToMany(mappedBy = "model", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<SmartFeatureEntity> features = new ArrayList<>();

    public UUID getId() { return id; }
    public List<SmartFeatureEntity> getFeatures() { return features; }

    public void addFeature(SmartFeatureEntity feature) {
        feature.setModel(this);
        features.add(feature);
    }

    public void removeFeature(SmartFeatureEntity feature) {
        features.remove(feature);
        feature.setModel(null);
    }
}
