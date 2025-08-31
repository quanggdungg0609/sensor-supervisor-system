package org.quangdung.domain.usecase.interfaces;

import java.util.Map;

import org.quangdung.domain.model.ThresholdKey;
import org.quangdung.domain.model.ThresholdModel;

import io.smallrye.mutiny.Uni;

public interface IGetAllThresholdsUseCase {
    Uni<Map<ThresholdKey, ThresholdModel>> execute(String clientId);
}
