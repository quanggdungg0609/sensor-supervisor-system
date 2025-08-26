package org.quangdung.domain.usecase.interfaces;

import org.quangdung.infrastructure.entity.ThresholdEntity;

import io.smallrye.mutiny.Uni;

public interface ISaveThresholdUseCase {
    Uni<Boolean> execute(String clientId, ThresholdEntity entity);
}
