package org.quangdung.infrastructure.repositories;

import java.util.Map;

import org.quangdung.infrastructure.component.redis.RedisRepository;
import org.quangdung.infrastructure.entity.ThresholdEntity;
import org.quangdung.infrastructure.repositories.interfaces.IThresholdRedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ThresholdRedisRepository implements IThresholdRedisRepository {
    @Inject
    private RedisRepository redisRepository;


    @Override
    public Uni<Boolean> saveThreshold(String clientId, ThresholdEntity.ThresholdLevel thresholdLevel, ThresholdEntity.ThresholdType thresholdType, String message, Map<String, Double> threshold) {
        ThresholdEntity thresholdEntity = ThresholdEntity.builder()
            .clientId(clientId)
            .thresholdLevel(thresholdLevel)
            .thresholdType(thresholdType)
            .message(message)
            .threshold(threshold)
            .build();
        return redisRepository.saveThreshold(thresholdEntity);
    }
    
    
    @Override
    public Uni<ThresholdEntity> getThreshold(String clientId, ThresholdEntity.ThresholdLevel thresholdLevel, ThresholdEntity.ThresholdType thresholdType) {
        return redisRepository.getThreshold(clientId, thresholdLevel, thresholdType);
    }
    
    
    @Override
    public Uni<Void> deleteThreshold(String clientId, ThresholdEntity.ThresholdLevel thresholdLevel, ThresholdEntity.ThresholdType thresholdType) {
        return redisRepository.deleteThreshold(clientId, thresholdLevel, thresholdType);
    }
    
    

    /**
     * Retrieves all threshold settings for a device across all levels
     * 
     * @param clientId The client ID of the device
     * @return Uni<Map<ThresholdEntity.ThresholdLevel, ThresholdEntity>> containing all threshold settings by level
     */
    @Override
    /**
     * Retrieves all threshold settings for a device
     * 
     * @param clientId The client ID of the device
     * @return Uni<Map<String, ThresholdEntity>> containing all threshold settings
     */
    public Uni<Map<String, ThresholdEntity>> getAllThreshold(String clientId) {
        return redisRepository.getAllThresholds(clientId);
    }
    

}
