package org.quangdung.domain.usecase.interfaces;

import org.quangdung.domain.model.DeviceInfoModel;

import io.smallrye.mutiny.Uni;

/**
 * Interface for retrieving device information by client ID
 */
public interface IGetDeviceByClientIdUseCase {
    /**
     * Retrieves device information by client ID from the device service
     * 
     * @param clientId The client ID of the device to retrieve information for
     * @return Uni<DeviceInfoModel> containing the device information
     */
    Uni<DeviceInfoModel> execute(String clientId);
}