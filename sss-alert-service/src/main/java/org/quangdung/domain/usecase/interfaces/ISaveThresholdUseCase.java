package org.quangdung.domain.usecase.interfaces;

import org.quangdung.domain.model.ThresholdModel;

import io.smallrye.mutiny.Uni;

public interface ISaveThresholdUseCase {
    Uni<Boolean> execute(String deviceUuid, ThresholdModel threshold);
}
