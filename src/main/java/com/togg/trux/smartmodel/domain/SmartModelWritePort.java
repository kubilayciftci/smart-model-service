package com.togg.trux.smartmodel.domain;

public interface SmartModelWritePort {
    void persist(SmartModelEntity model);
    void delete(SmartModelEntity model);
}
