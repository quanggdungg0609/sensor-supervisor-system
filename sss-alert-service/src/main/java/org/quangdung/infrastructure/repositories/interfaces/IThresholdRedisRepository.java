package org.quangdung.infrastructure.repositories.interfaces;

import java.util.Map;

import org.quangdung.infrastructure.entity.ThresholdEntity;

import io.smallrye.mutiny.Uni;

public interface IThresholdRedisRepository {
    Uni<Boolean> saveThreshold(String clientId, ThresholdEntity.ThresholdLevel thresholdLevel, ThresholdEntity.ThresholdType thresholdType, String message, Map<String, Double> threshold);
    Uni<Map<String, ThresholdEntity>> getAllThreshold(String clientId);
    Uni<ThresholdEntity> getThreshold(String clientId, ThresholdEntity.ThresholdLevel thresholdLevel, ThresholdEntity.ThresholdType thresholdType);
    Uni<Void> deleteThreshold(String clientId, ThresholdEntity.ThresholdLevel thresholdLevel, ThresholdEntity.ThresholdType thresholdType);
}
