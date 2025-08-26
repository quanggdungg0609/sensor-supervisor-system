package org.quangdung.infrastructure.repositories;

import java.util.Map;

import org.quangdung.infrastructure.repositories.interfaces.IThresholdRedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class ThresholdRedisRepository implements IThresholdRedisRepository {

    @Override
    public Uni<Void> saveThreshold(String deviceUuid, Map<String, Double> threshold) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'saveThreshold'");
    }
    
}
