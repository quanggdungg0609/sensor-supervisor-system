package com.quangdung.domain.usecase.implement;

import com.quangdung.application.dto.response.DeviceInfo;
import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.domain.usecase.interfaces.IGetDeviceByClientIdUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of the get device by client ID use case
 */
@ApplicationScoped
public class GetDeviceByClientIdUseCase implements IGetDeviceByClientIdUseCase {
    private final IDeviceRepository deviceRepository;

    @Inject
    public GetDeviceByClientIdUseCase(IDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    /**
     * Execute get device by client ID operation
     * @param clientId Client ID of the device to retrieve
     * @return Device information if found
     */
    @Override
    public Uni<DeviceInfo> execute(String clientId) {
        return deviceRepository.getDeviceByClientId(clientId).onItem().transform(
            device -> DeviceInfo.builder()
                .deviceUuid(device.getDeviceUuid().toString())
                .deviceName(device.getDeviceName())
                .mqttUsername(device.getMqttUsername())
                .clientId(device.getClientId())
                .build()
        );
    }
}