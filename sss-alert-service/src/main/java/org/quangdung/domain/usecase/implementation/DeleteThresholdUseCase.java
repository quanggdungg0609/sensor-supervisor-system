package org.quangdung.domain.usecase.implementation;

import org.quangdung.domain.model.ThresholdModel.ThresholdLevel;
import org.quangdung.domain.model.ThresholdModel.ThresholdType;
import org.quangdung.domain.usecase.interfaces.IDeleteThresholdUseCase;
import org.quangdung.infrastructure.entity.ThresholdEntity;
import org.quangdung.infrastructure.repositories.ThresholdRedisRepository;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DeleteThresholdUseCase implements IDeleteThresholdUseCase {
    @Inject
    private ThresholdRedisRepository thresholdRedisRepository;

    @Override
    public Uni<Void> execute(String clientId, ThresholdLevel level, ThresholdType type) {
        return thresholdRedisRepository.deleteThreshold(
            clientId, 
            switch(level){
                case WARNING -> ThresholdEntity.ThresholdLevel.WARNING;
                case CRITICAL -> ThresholdEntity.ThresholdLevel.CRITICAL;
            },
            switch(type){
                case UPPER -> ThresholdEntity.ThresholdType.UPPER;
                case LOWER -> ThresholdEntity.ThresholdType.LOWER;
            }
        );
    }
    
}
