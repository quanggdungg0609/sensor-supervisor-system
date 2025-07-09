package org.quangdung.domain.use_case.interfaces;

import org.quangdung.domain.entity.DeviceInfo;

import io.smallrye.mutiny.Uni;

public interface IGetDeviceDetailsByClientIdUseCase {
    Uni<DeviceInfo> execute(String clientId);
}
