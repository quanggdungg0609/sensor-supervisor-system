package org.quangdung.infrastructure.component.redis;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.quangdung.infrastructure.entity.ThresholdEntity;

import io.quarkus.redis.datasource.ReactiveRedisDataSource;
import io.quarkus.redis.datasource.hash.ReactiveHashCommands;
import io.smallrye.mutiny.Uni;


@ApplicationScoped
public class RedisRepository {

    private String THRESHOLD_KEY = "THRESHOLD";

    private final ReactiveHashCommands<String, String, ThresholdEntity> commands;

    @Inject
    public RedisRepository(ReactiveRedisDataSource dataSource) {
        this.commands = dataSource.hash(ThresholdEntity.class);
    }

    public Uni<Boolean> saveThreshold(ThresholdEntity entity){
        return commands.hset(THRESHOLD_KEY, entity.getClientId(), entity);
    }


    public Uni<ThresholdEntity> getThreshold(String clientId){
        return commands.hget(THRESHOLD_KEY, clientId);
    }
}
