package com.quangdung.domain.repository;

import com.quangdung.domain.entity.Device;
import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;

import java.util.List;

public interface IDeviceRepository {
    Uni<Device> createDevice(Device device);
    Uni<Boolean> isMqttUsernameExist(String mqttUsername);
    Uni<Device> getDeviceByUuid(String deviceUuid);
    
    /**
     * Update existing device information
     * @param device Device entity to update
     * @return Updated device entity
     */
    Uni<Device> updateDevice(Device device);
    
    /**
     * Get all devices with pagination support
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return List of devices for the specified page
     */
    Uni<List<Device>> getAllDevicesWithPagination(int page, int size);
    
    /**
     * Get total count of devices
     * @return Total number of devices in database
     */
    Uni<Long> getTotalDevicesCount();
}
