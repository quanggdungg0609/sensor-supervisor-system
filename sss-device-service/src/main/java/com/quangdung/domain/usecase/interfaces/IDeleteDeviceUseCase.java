package com.quangdung.domain.usecase.interfaces;

import io.smallrye.mutiny.Uni;

/**
 * Interface for device deletion use case
 */
public interface IDeleteDeviceUseCase {
    /**
     * Deletes a device by its UUID and its associated MQTT account
     * 
     * @param deviceUuid The UUID of the device to delete
     * @return Boolean indicating success or failure of the deletion
     */
    Uni<Boolean> execute(String deviceUuid);
}