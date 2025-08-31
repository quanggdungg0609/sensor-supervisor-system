package org.quangdung.domain.usecase.implementation;

import org.quangdung.domain.usecase.interfaces.IAddAlertEmailUseCase;
import org.quangdung.infrastructure.component.redis.RedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of IAddAlertEmailUseCase for adding an email address to the alert notification list
 */
@ApplicationScoped
public class AddAlertEmailUseCase implements IAddAlertEmailUseCase {
    
    @Inject
    private RedisRepository redisRepository;
    
    /**
     * Adds an email address to the alert notification list
     * 
     * @param email The email address to add
     * @return Uni<Boolean> indicating success or failure
     */
    @Override
    public Uni<Boolean> execute(String email) {
        return redisRepository.setAlertEmail(email);
    }
}