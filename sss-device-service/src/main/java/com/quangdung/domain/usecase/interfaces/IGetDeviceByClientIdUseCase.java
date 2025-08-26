package com.quangdung.domain.usecase.interfaces;

import com.quangdung.application.dto.response.DeviceInfo;

import io.smallrye.mutiny.Uni;

/**
 * Interface for getting device by client ID use case
 */
public interface IGetDeviceByClientIdUseCase {
    /**
     * Execute get device by client ID operation
     * @param clientId Client ID of the device to retrieve
     * @return Device information if found
     */
    Uni<DeviceInfo> execute(String clientId);
}