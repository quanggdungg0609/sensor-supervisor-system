package org.quangdung.domain.usecase.interfaces;


import org.quangdung.domain.model.ThresholdModel.ThresholdLevel;
import org.quangdung.domain.model.ThresholdModel.ThresholdType;

import io.smallrye.mutiny.Uni;

public interface IDeleteThresholdUseCase {
    Uni<Void> execute(String clientId, ThresholdLevel level, ThresholdType type);
}
