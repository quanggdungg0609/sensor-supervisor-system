package com.quangdung.domain.usecase.interfaces;

import com.quangdung.application.dto.response.DeviceInfo;

import io.smallrye.mutiny.Uni;

public interface IGetDeviceByUuidUseCase {
    Uni<DeviceInfo> execute(String deviceUuid);
}
