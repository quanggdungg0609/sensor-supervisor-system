package com.quangdung.infrastructure.repository;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jboss.logging.Logger;

import com.quangdung.core.exception.CreateDeviceException;
import com.quangdung.core.exception.DeviceNotFoundException;
import com.quangdung.core.exception.GetDeviceByUuidException;
import com.quangdung.core.exception.IsMqttUsernameExistsException;
import com.quangdung.domain.entity.Device;
import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.infrastructure.entity.device_entity.DeviceEntity;

import io.quarkus.panache.common.Page;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class DeviceRepository implements IDeviceRepository{
    private final Logger log;

    public DeviceRepository(Logger log){
        this.log = log;
    }

    @Override
    public Uni<Boolean> isMqttUsernameExist(String mqttUsername) {
        return DeviceEntity.find("mqttUsername", mqttUsername).firstResult()
            .onItem().transform(deviceEntity -> deviceEntity != null)
            .onFailure().transform(throwable -> {
                log.error(throwable);
                return new IsMqttUsernameExistsException("Problem when checking mqtt username exists", throwable);
            });
    }

    @Override
    public Uni<Device> createDevice(Device device) {
        return device.toEntity().persist().map(entity -> Device.fromEntity((DeviceEntity) entity))
            .onFailure().transform(throwable -> {
                log.error(throwable);
                return new CreateDeviceException("Problem when creating device", throwable);
            });
    }

    @Override
    public Uni<Device> getDeviceByUuid(String deviceUuid) {
        UUID uuid;
        try {
        uuid = UUID.fromString(deviceUuid);
        } catch (IllegalArgumentException e) {
            log.errorf(e, "Invalid UUID format received: %s", deviceUuid);
            return Uni.createFrom().failure(new CreateDeviceException("Invalid device UUID format", e));
        }
        return DeviceEntity.findById(uuid)
            .onItem().ifNotNull().transform(deviceEntity -> Device.fromEntity((DeviceEntity) deviceEntity))
            .onItem().ifNull().failWith(new DeviceNotFoundException("Device with UUID " + deviceUuid + " not found."))
            .onFailure().transform(throwable -> {
                log.error(throwable);
                if (throwable instanceof DeviceNotFoundException) {
                    return throwable;
                }
                return new GetDeviceByUuidException("Problem when getting device by UUID", throwable);
            });
    }

    /**
     * Get all devices with pagination support
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return List of devices for the specified page
     */
    @Override
    public Uni<List<Device>> getAllDevicesWithPagination(int page, int size) {
        return DeviceEntity.findAll()
            .page(Page.of(page, size))
            .list()
            .onItem().transform(entities -> 
                entities.stream()
                    .map(entity -> Device.fromEntity((DeviceEntity) entity))
                    .collect(Collectors.toList())
            )
            .onFailure().transform(throwable -> {
                log.error("Error getting devices with pagination", throwable);
                return new RuntimeException("Problem when getting devices with pagination", throwable);
            });
    }

    /**
     * Get total count of devices
     * @return Total number of devices in database
     */
    @Override
    public Uni<Long> getTotalDevicesCount() {
        return DeviceEntity.count()
            .onFailure().transform(throwable -> {
                log.error("Error getting total devices count", throwable);
                return new RuntimeException("Problem when getting total devices count", throwable);
            });
    }
}
