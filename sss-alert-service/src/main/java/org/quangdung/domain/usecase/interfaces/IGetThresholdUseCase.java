package org.quangdung.domain.usecase.interfaces;

import org.quangdung.domain.model.ThresholdModel;

import io.smallrye.mutiny.Uni;

public interface IGetThresholdUseCase {
    Uni<ThresholdModel> execute(String clientId);
}
