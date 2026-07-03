package com.togg.trux.smartmodel.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "smart_feature")
public class SmartFeatureEntity extends CatalogDescriptorEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "smart_model_id", nullable = false)
    private SmartModelEntity model;

    public UUID getId() { return id; }
    public SmartModelEntity getModel() { return model; }
    public void setModel(SmartModelEntity model) { this.model = model; }
}
