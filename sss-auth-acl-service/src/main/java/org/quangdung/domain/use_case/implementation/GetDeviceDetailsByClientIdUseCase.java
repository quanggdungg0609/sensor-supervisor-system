package org.quangdung.domain.use_case.implementation;

import org.quangdung.domain.entity.DeviceInfo;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IGetDeviceDetailsByClientIdUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetDeviceDetailsByClientIdUseCase implements IGetDeviceDetailsByClientIdUseCase{
    private final IMqttRepository mqttRepository;

    @Inject
    public GetDeviceDetailsByClientIdUseCase(IMqttRepository mqttRepository) {
        this.mqttRepository = mqttRepository;
    }


    @Override
    public Uni<DeviceInfo> execute(String clientId) {
        return mqttRepository.getDeviceInfoByClientId(clientId).onItem().transform(deviceDetail ->{
            return DeviceInfo.builder()
                .clientId(clientId)
                .deviceUuid(deviceDetail.getDeviceUuid())
                .deviceName(deviceDetail.getDeviceName())
                .mqttUsername(deviceDetail.getMqttUsername())
                .build();
        });
    }
    
}
