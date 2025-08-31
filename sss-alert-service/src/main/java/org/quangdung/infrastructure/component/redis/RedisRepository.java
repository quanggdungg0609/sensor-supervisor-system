package org.quangdung.infrastructure.component.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.quangdung.infrastructure.entity.ThresholdEntity;
import org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdLevel;
import org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdType;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.quarkus.redis.datasource.value.ReactiveValueCommands;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.Multi;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;

@ApplicationScoped
public class RedisRepository {

    private String THRESHOLD_KEY = "THRESHOLD";
    private String ALERT_EMAIL_KEY = "ALERT_EMAIL";

    private final ReactiveHashCommands<String, String, ThresholdEntity> thresholdCommands;
    private final ReactiveHashCommands<String, String, String> alertEmailCommands;

    @Inject
    public RedisRepository(ReactiveRedisDataSource dataSource) {
        this.thresholdCommands = dataSource.hash(ThresholdEntity.class);
        this.alertEmailCommands = dataSource.hash(String.class);
    }

    /**
     * Saves threshold settings for a device with specific level
     * 
     * @param entity The threshold entity to save
     * @return Uni<Boolean> indicating success or failure
     */
    public Uni<Boolean> saveThreshold(ThresholdEntity entity) {
        String compositeKey = createCompositeKey(entity.getClientId(), entity.getThresholdLevel(), entity.getThresholdType());
        return thresholdCommands.hset(THRESHOLD_KEY, compositeKey, entity);
    }

    /**
     * Retrieves threshold settings for a device with specific level and type
     * 
     * @param clientId The client ID of the device
     * @param level The threshold level to retrieve
     * @param type The threshold type to retrieve
     * @return Uni<ThresholdEntity> containing the threshold settings
     */
    public Uni<ThresholdEntity> getThreshold(String clientId, ThresholdLevel level, ThresholdType type) {
        String compositeKey = createCompositeKey(clientId, level, type);
        return thresholdCommands.hget(THRESHOLD_KEY, compositeKey);
    }

    /**
     * Retrieves threshold settings for a device with specific level
     * 
     * @param clientId The client ID of the device
     * @param level The threshold level to retrieve
     * @return Uni<ThresholdEntity> containing the threshold settings
     */
    public Uni<ThresholdEntity> getThreshold(String clientId, ThresholdLevel level) {
        String compositeKey = createCompositeKey(clientId, level);
        return thresholdCommands.hget(THRESHOLD_KEY, compositeKey);
    }

    /**
     * Retrieves all threshold settings for a device across all levels and types
     * 
     * @param clientId The client ID of the device
     * @return Uni<Map<ThresholdLevel, ThresholdEntity>> containing all threshold settings by level
     */
    /**
     * Retrieves all threshold settings for a device
     * 
     * @param clientId The client ID of the device
     * @return Uni<Map<String, ThresholdEntity>> containing all threshold settings
     */
    public Uni<Map<String, ThresholdEntity>> getAllThresholds(String clientId) {
        // Get all keys in the hash
        return thresholdCommands.hkeys(THRESHOLD_KEY)
                .onItem().transformToMulti(keys -> Multi.createFrom().iterable(keys))
                .filter(key -> key.startsWith(clientId + ":"))
                .onItem().transformToUniAndMerge(key -> thresholdCommands.hget(THRESHOLD_KEY, key)
                        .onItem().transform(entity -> Map.entry(key, entity)))
                .collect().asMap(Map.Entry::getKey, Map.Entry::getValue);
    }

    /**
     * Deletes threshold settings for a device with specific level and type
     * 
     * @param clientId The client ID of the device
     * @param level The threshold level to delete
     * @param type The threshold type to delete
     * @return Uni<Void> indicating completion
     */
    public Uni<Void> deleteThreshold(String clientId, ThresholdLevel level, ThresholdType type) {
        String compositeKey = createCompositeKey(clientId, level, type);
        return thresholdCommands.hdel(THRESHOLD_KEY, compositeKey).replaceWithVoid();
    }
    
    /**
     * Deletes threshold settings for a device with specific level (for backward compatibility)
     * 
     * @param clientId The client ID of the device
     * @param level The threshold level to delete
     * @return Uni<Void> indicating completion
     */
    public Uni<Void> deleteThreshold(String clientId, ThresholdLevel level) {
        String compositeKey = createCompositeKey(clientId, level);
        return thresholdCommands.hdel(THRESHOLD_KEY, compositeKey).replaceWithVoid();
    }

    /**
     * Deletes all threshold settings for a device across all levels
     * 
     * @param clientId The client ID of the device
     * @return Uni<Void> indicating completion
     */
    public Uni<Void> deleteAllThresholds(String clientId) {
        return thresholdCommands.hkeys(THRESHOLD_KEY)
                .onItem().transformToMulti(keys -> Multi.createFrom().iterable(keys))
                .filter(key -> key.startsWith(clientId + ":"))
                .collect().asList()
                .onItem().transformToUni(keys -> {
                    if (keys.isEmpty()) {
                        return Uni.createFrom().voidItem();
                    }
                    return thresholdCommands.hdel(THRESHOLD_KEY, keys.toArray(new String[0])).replaceWithVoid();
                });
    }

    /**
     * Creates a composite key from clientId, threshold level and threshold type
     * 
     * @param clientId The client ID
     * @param level The threshold level
     * @param type The threshold type
     * @return String composite key
     */
    private String createCompositeKey(String clientId, ThresholdLevel level, ThresholdType type) {
        return clientId + ":" + level.name() + ":" + type.name();
    }
    
    /**
     * Creates a composite key from clientId and threshold level (for backward compatibility)
     * 
     * @param clientId The client ID
     * @param level The threshold level
     * @return String composite key
     */
    private String createCompositeKey(String clientId, ThresholdLevel level) {
        return clientId + ":" + level.name();
    }
    
    /**
     * Adds an email address to the notification list for threshold alerts
     * 
     * @param email The email address to add
     * @return Uni<Boolean> indicating success or failure
     */
    public Uni<Boolean> setAlertEmail(String email) {
        return alertEmailCommands.hset(ALERT_EMAIL_KEY, email, email);
    }
    
    /**
     * Retrieves all notification email addresses for threshold alerts
     * 
     * @return Uni<Set<String>> containing all email addresses
     */
    public Uni<Set<String>> getAlertEmails() {
        return alertEmailCommands.hgetall(ALERT_EMAIL_KEY)
                .onItem().transform(map -> Set.copyOf(map.values()));
    }
    
    /**
     * Deletes a specific email address from the notification list
     * 
     * @param email The email address to delete
     * @return Uni<Long> indicating the number of emails removed (0 or 1)
     */
    public Uni<Integer> deleteAlertEmail(String email) {
        return alertEmailCommands.hdel(ALERT_EMAIL_KEY, email);
    }
}
