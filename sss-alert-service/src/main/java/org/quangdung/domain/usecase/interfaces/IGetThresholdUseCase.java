package org.quangdung.domain.usecase.interfaces;

import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.model.ThresholdModel.ThresholdLevel;
import org.quangdung.domain.model.ThresholdModel.ThresholdType;

import io.smallrye.mutiny.Uni;


public interface IGetThresholdUseCase {
    
    /**
     * Retrieves threshold settings for a device with specific level
     * 
     * @param deviceUuid The device UUID
     * @param level The threshold level to retrieve
     * @return Uni<ThresholdModel> containing the threshold settings
     */
    Uni<ThresholdModel> execute(String clientId, ThresholdLevel level, ThresholdType type);
}
