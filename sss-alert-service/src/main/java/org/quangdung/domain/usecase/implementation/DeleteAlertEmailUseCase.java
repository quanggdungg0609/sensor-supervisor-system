package org.quangdung.domain.usecase.implementation;

import org.quangdung.domain.usecase.interfaces.IDeleteAlertEmailUseCase;
import org.quangdung.infrastructure.component.redis.RedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of IDeleteAlertEmailUseCase for deleting an email address from the alert notification list
 */
@ApplicationScoped
public class DeleteAlertEmailUseCase implements IDeleteAlertEmailUseCase {
    
    @Inject
    private RedisRepository redisRepository;
    
    /**
     * Deletes an email address from the alert notification list
     * 
     * @param email The email address to delete
     * @return Uni<Long> indicating the number of emails removed (0 or 1)
     */
    @Override
    public Uni<Integer> execute(String email) {
        return redisRepository.deleteAlertEmail(email);
    }
}