package com.quangdung.domain.usecase.implement;

import jakarta.inject.Inject;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;


import com.quangdung.domain.entity.Device;
import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.infrastructure.dao.mqtt_dao.MqttDAO;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.quangdung.core.exception.CreateMqttAccountException;
import com.quangdung.application.dto.response.CreatedDeviceInfo;
import com.quangdung.domain.usecase.interfaces.ICreateDeviceUseCase;
import com.quangdung.infrastructure.entity.mqtt.CreateMqttAccountRequest;



@ApplicationScoped
public class CreateDeviceUseCase implements ICreateDeviceUseCase{
    @Inject
    private Logger log;

    @Inject
    private IDeviceRepository deviceRepository;

    @Inject
    @RestClient
    private MqttDAO mqttDAO;


    @Override
    public Uni<CreatedDeviceInfo> execute(String deviceName, String mqttUsername) {
        log.infof("mqttUsername: {}", mqttUsername);
        Device newDevice = Device.builder()
            .deviceName(deviceName)
            .mqttUsername(mqttUsername)
            .status(Device.DeviceStatus.INACTIVE)
            .build();
        return deviceRepository.createDevice(newDevice).flatMap(savedDevice ->{
            log.info(savedDevice.toString());
            return mqttDAO.createMqttAccount(
                CreateMqttAccountRequest.builder()
                .deviceUuid(savedDevice.getDeviceUuid().toString())
                .mqttUsername(savedDevice.getMqttUsername())
                .build()
            ).onFailure().transform(throwable ->{
                log.error(throwable);
                return new CreateMqttAccountException(throwable.getMessage());
            })
            .onItem().transform(
                response -> CreatedDeviceInfo.builder()
                    .deviceName(deviceName)
                    .mqttUsername(response.getMqttUsername())
                    .mqttPassword(response.getMqttPassword())
                    .clientId(response.getClientId())
                    .build()
            );
        });
    }
    
}
