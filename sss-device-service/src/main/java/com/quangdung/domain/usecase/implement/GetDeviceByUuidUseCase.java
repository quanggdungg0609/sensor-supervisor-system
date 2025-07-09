package com.quangdung.domain.usecase.implement;

import com.quangdung.application.dto.response.DeviceInfo;
import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.domain.usecase.interfaces.IGetDeviceByUuidUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetDeviceByUuidUseCase implements IGetDeviceByUuidUseCase{
    private final IDeviceRepository deviceRepository;

    @Inject
    public GetDeviceByUuidUseCase(IDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Uni<DeviceInfo> execute(String deviceUuid) {
        return deviceRepository.getDeviceByUuid(deviceUuid).onItem().transform(
            device -> DeviceInfo.builder()
                .deviceUuid(device.getDeviceUuid().toString())
                .deviceName(device.getDeviceName())
                .mqttUsername(device.getMqttUsername())
                .build()
        );
    }
    
}
