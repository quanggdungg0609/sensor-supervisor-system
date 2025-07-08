package com.quangdung.infrastructure.repository;

import org.jboss.logging.Logger;

import com.quangdung.core.exception.CreateDeviceException;
import com.quangdung.core.exception.IsMqttUsernameExistsException;
import com.quangdung.domain.entity.Device;
import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.infrastructure.entity.device_entity.DeviceEntity;

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
}
