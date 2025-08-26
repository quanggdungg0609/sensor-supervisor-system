package org.quangdung.infrastructure.repositories.interfaces;

import java.util.Map;

import io.smallrye.mutiny.Uni;

public interface IThresholdRedisRepository {
    Uni<Void> saveThreshold(String deviceUuid, Map<String, Double> threshold);
}
