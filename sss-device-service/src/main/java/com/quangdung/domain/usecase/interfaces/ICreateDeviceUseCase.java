package com.quangdung.domain.usecase.interfaces;

import com.quangdung.application.dto.response.CreatedDeviceInfo;

import io.smallrye.mutiny.Uni;

public interface ICreateDeviceUseCase {
    Uni<CreatedDeviceInfo> execute(String deviceName, String mqttUsername);
}
