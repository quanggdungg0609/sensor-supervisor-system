package org.quangdung.domain.usecase.implementation;

import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.model.ThresholdModel.ThresholdLevel;
import org.quangdung.domain.model.ThresholdModel.ThresholdType;
import org.quangdung.domain.usecase.interfaces.IGetThresholdUseCase;
import org.quangdung.infrastructure.entity.ThresholdEntity;
import org.quangdung.infrastructure.repositories.interfaces.IThresholdRedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GetThresholdUseCase implements IGetThresholdUseCase {
    private static final Logger LOG = Logger.getLogger(GetThresholdUseCase.class);
    
    @Inject
    private IThresholdRedisRepository thresholdRedisRepository;
    
    /**
     * Retrieves threshold settings for a device with specific level
     * 
     * @param clientId The client ID of the device
     * @param level The threshold level to retrieve
     * @return Uni<ThresholdModel> containing the threshold settings
     * @throws NotFoundException if threshold is not found
     */
    @Override
    public Uni<ThresholdModel> execute(String clientId, ThresholdLevel level, ThresholdType type) {
        LOG.infof("Getting threshold for clientId: %s with level: %s", clientId, level);
        
        return thresholdRedisRepository.getThreshold(
            clientId, 
            switch(level) {
                case CRITICAL -> ThresholdEntity.ThresholdLevel.CRITICAL;
                case WARNING -> ThresholdEntity.ThresholdLevel.WARNING;
            },
            switch(type) {
                case UPPER -> ThresholdEntity.ThresholdType.UPPER;
                case LOWER -> ThresholdEntity.ThresholdType.LOWER;
            }
        ).onItem().transform(thresholdEntity -> {
            // Check if threshold entity is null (not found)
            if (thresholdEntity == null) {
                LOG.infof("Threshold not found for clientId: %s with level: %s", clientId, level);
                throw new NotFoundException("Threshold not found");
            }
            
            // If threshold exists, map it to model
            return ThresholdModel.builder()
                .clientId(thresholdEntity.getClientId())
                .message(thresholdEntity.getMessage())
                .thresholdType(
                    switch(thresholdEntity.getThresholdType()) {
                        case UPPER -> ThresholdModel.ThresholdType.UPPER;
                        case LOWER -> ThresholdModel.ThresholdType.LOWER;
                    }
                )
                .thresholdLevel(
                    switch(thresholdEntity.getThresholdLevel()) {
                        case CRITICAL -> ThresholdModel.ThresholdLevel.CRITICAL;
                        case WARNING -> ThresholdModel.ThresholdLevel.WARNING;
                    }
                )
                .threshold(thresholdEntity.getThreshold())
                .build();
        });
    }
}
