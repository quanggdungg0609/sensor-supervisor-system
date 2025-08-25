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

    /**
     * Execute device creation process including MQTT account creation and clientId update
     * @param deviceName Name of the device to create
     * @param mqttUsername MQTT username for the device
     * @return CreatedDeviceInfo containing device and MQTT account information
     */
    @Override
    public Uni<CreatedDeviceInfo> execute(String deviceName, String mqttUsername) {
        log.infof("Creating device with mqttUsername: {}", mqttUsername);
        
        // Create initial device without clientId
        Device newDevice = Device.builder()
            .deviceName(deviceName)
            .mqttUsername(mqttUsername)
            .status(Device.DeviceStatus.INACTIVE)
            .build();
            
        return deviceRepository.createDevice(newDevice)
            .flatMap(savedDevice -> {
                log.infof("Device saved to database: {}", savedDevice.toString());
                
                // Create MQTT account
                return mqttDAO.createMqttAccount(
                    CreateMqttAccountRequest.builder()
                        .deviceUuid(savedDevice.getDeviceUuid().toString())
                        .mqttUsername(savedDevice.getMqttUsername())
                        .build()
                )
                .onFailure().transform(throwable -> {
                    log.error("Failed to create MQTT account", throwable);
                    return new CreateMqttAccountException(throwable.getMessage());
                })
                .flatMap(response -> {
                    log.infof("MQTT account created successfully with clientId: {}", response.getClientId());
                    log.info(response.getClientId());
                    // Update device with clientId from MQTT response
                    Device updatedDevice = Device.builder()
                        .deviceUuid(savedDevice.getDeviceUuid())
                        .deviceName(savedDevice.getDeviceName())
                        .mqttUsername(savedDevice.getMqttUsername())
                        .clientId(response.getClientId())
                        .status(Device.DeviceStatus.INACTIVE) // Keep INACTIVE until device activation
                        .build();
                    
                    // Update device in database with clientId
                    return deviceRepository.updateDevice(updatedDevice)
                        .onItem().transform(finalDevice -> {
                            log.infof("Device updated with clientId: {}", finalDevice.getClientId());
                            
                            // Return CreatedDeviceInfo with all information
                            return CreatedDeviceInfo.builder()
                                .deviceName(deviceName)
                                .mqttUsername(response.getMqttUsername())
                                .mqttPassword(response.getMqttPassword())
                                .clientId(response.getClientId())
                                .build();
                        });
                });
            });
    }
}
