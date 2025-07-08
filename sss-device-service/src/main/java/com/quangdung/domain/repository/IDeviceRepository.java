package com.quangdung.domain.repository;

import com.quangdung.domain.entity.Device;

import io.smallrye.mutiny.Uni;

public interface IDeviceRepository {
    Uni<Device> createDevice(Device device);
    Uni<Boolean> isMqttUsernameExist(String mqttUsername);
}
