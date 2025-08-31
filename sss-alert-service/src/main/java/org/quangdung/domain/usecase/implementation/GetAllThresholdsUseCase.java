package org.quangdung.domain.usecase.implementation;

import java.util.Map;
import java.util.stream.Collectors;

import org.quangdung.domain.model.ThresholdKey;
import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.model.ThresholdModel.ThresholdLevel;
import org.quangdung.domain.usecase.interfaces.IGetAllThresholdsUseCase;
import org.quangdung.infrastructure.repositories.interfaces.IThresholdRedisRepository;
import org.quangdung.infrastructure.entity.ThresholdEntity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetAllThresholdsUseCase implements IGetAllThresholdsUseCase {
    @Inject
    private IThresholdRedisRepository thresholdRedisRepository;

    /**
     * Retrieves all threshold settings for a device across all levels and types
     * 
     * @param clientId The client ID of the device
     * @return Uni<Map<ThresholdKey, ThresholdModel>> containing all threshold settings
     */
    @Override
    public Uni<Map<ThresholdKey, ThresholdModel>> execute(String clientId) {
        return thresholdRedisRepository.getAllThreshold(clientId)
                .onItem().transform(thresholdMap -> {
                    // Convert from Map<String, ThresholdEntity> to Map<ThresholdKey, ThresholdModel>
                    return thresholdMap.values().stream()
                            .collect(Collectors.toMap(
                                entity -> new ThresholdKey(
                                    ThresholdModel.ThresholdLevel.valueOf(entity.getThresholdLevel().name()),
                                    ThresholdModel.ThresholdType.valueOf(entity.getThresholdType().name())
                                ),
                                this::mapToThresholdModel
                            ));
                });
    }
    
    /**
     * Maps ThresholdEntity to ThresholdModel
     * 
     * @param entity The ThresholdEntity to map
     * @return ThresholdModel mapped from the entity
     */
    private ThresholdModel mapToThresholdModel(ThresholdEntity entity) {
        return ThresholdModel.builder()
                .clientId(entity.getClientId())
                .message(entity.getMessage())
                .thresholdLevel(ThresholdModel.ThresholdLevel.valueOf(entity.getThresholdLevel().name()))
                .thresholdType(entity.getThresholdType() != null ? 
                    ThresholdModel.ThresholdType.valueOf(entity.getThresholdType().name()) : null)
                .threshold(entity.getThreshold())
                .build();
    }
}
