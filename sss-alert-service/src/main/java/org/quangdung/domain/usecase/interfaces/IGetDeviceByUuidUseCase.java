package org.quangdung.domain.usecase.interfaces;

import org.quangdung.domain.model.DeviceInfoModel;

import io.smallrye.mutiny.Uni;

/**
 * Interface for retrieving device information by device UUID
 */
public interface IGetDeviceByUuidUseCase {
    /**
     * Retrieves device information by device UUID from the device service
     * 
     * @param deviceUuid The device UUID to retrieve information for
     * @return Uni<DeviceInfoModel> containing the device information
     */
    Uni<DeviceInfoModel> execute(String deviceUuid);
}