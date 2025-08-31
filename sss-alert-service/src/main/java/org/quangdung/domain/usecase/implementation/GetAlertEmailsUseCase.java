package org.quangdung.domain.usecase.implementation;

import java.util.Set;

import org.quangdung.domain.usecase.interfaces.IGetAlertEmailsUseCase;
import org.quangdung.infrastructure.component.redis.RedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of IGetAlertEmailsUseCase for retrieving all alert email addresses
 */
@ApplicationScoped
public class GetAlertEmailsUseCase implements IGetAlertEmailsUseCase {
    
    @Inject
    private RedisRepository redisRepository;
    
    /**
     * Retrieves all email addresses registered for alerts
     * 
     * @return Uni<Set<String>> containing all registered email addresses
     */
    @Override
    public Uni<Set<String>> execute() {
        return redisRepository.getAlertEmails();
    }
}