package org.quangdung.domain.usecase.interfaces;

import java.util.Map;

import org.quangdung.domain.model.DeviceInfoModel;
import org.quangdung.domain.model.ThresholdModel;

import io.smallrye.mutiny.Uni;

public interface ISendMailAlertUseCase {
    Uni<Void> execute(Map<String, Double> currentValues, ThresholdModel threshold, DeviceInfoModel deviceInfo);
}
