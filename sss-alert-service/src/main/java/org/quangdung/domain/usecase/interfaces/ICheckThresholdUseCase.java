package org.quangdung.domain.usecase.interfaces;

import java.util.Map;

import org.quangdung.domain.model.ThresholdModel;

import io.smallrye.mutiny.Uni;

public interface ICheckThresholdUseCase {
    Uni<ThresholdModel> execute(String clientId, Map<String, Double> values);
}
