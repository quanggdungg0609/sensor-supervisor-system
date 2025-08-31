package org.quangdung.domain.usecase.implementation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.jboss.logging.Logger;
import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.usecase.interfaces.ISaveThresholdUseCase;
import org.quangdung.infrastructure.repositories.interfaces.IThresholdRedisRepository;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class SaveThresholdUseCase implements ISaveThresholdUseCase {

    private static final Logger LOG = Logger.getLogger(SaveThresholdUseCase.class);
    
    @Inject
    IThresholdRedisRepository thresholdRedisRepository;
    
    

    
    /**
     * Saves threshold settings for a device
     * 
     * @param deviceUuid The device UUID to save threshold settings for
     * @param threshold The threshold model containing threshold values
     * @return Uni<Boolean> indicating completion status
     */
    @Override
    public Uni<Boolean> execute(String clientId, ThresholdModel threshold) {
        LOG.infof("Saving threshold for clientId: %s", clientId);
        
        // First, get device information by UUI
        org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdLevel entityLevel = 
                    switch(threshold.getThresholdLevel()) {
                        case CRITICAL -> org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdLevel.CRITICAL;
                        case WARNING -> org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdLevel.WARNING;
        };

        org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdType entityType = 
                    switch(threshold.getThresholdType()) {
                        case UPPER -> org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdType.UPPER;
                        case LOWER -> org.quangdung.infrastructure.entity.ThresholdEntity.ThresholdType.LOWER;
        };
                
                // Save threshold with the retrieved clientId
        return thresholdRedisRepository.saveThreshold(
                    clientId,
                    entityLevel,
                    entityType,
                    threshold.getMessage(),
                    threshold.getThreshold()
                );
    }
}