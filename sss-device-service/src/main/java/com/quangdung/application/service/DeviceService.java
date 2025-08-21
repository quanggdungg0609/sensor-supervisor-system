package com.quangdung.application.service;

import org.jboss.logging.Logger;

import com.quangdung.application.dto.request.CreateDeviceRequest;
import com.quangdung.core.exception.InvalidMqttUsernameException;
import com.quangdung.core.exception.MqttUsernameAlreadyExistsException;
import com.quangdung.domain.usecase.interfaces.ICheckMqttUsernameExistsUseCase;
import com.quangdung.domain.usecase.interfaces.ICreateDeviceUseCase;
import com.quangdung.domain.usecase.interfaces.IGetDeviceByUuidUseCase;
import com.quangdung.domain.usecase.interfaces.IGetAllDevicesUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class DeviceService {
    private final Logger log;
    private final ICheckMqttUsernameExistsUseCase checkMqttUsernameExistsUseCase;
    private final ICreateDeviceUseCase createDeviceUseCase;
    private final IGetDeviceByUuidUseCase getDeviceByUuidUseCase;
    private final IGetAllDevicesUseCase getAllDevicesUseCase;

    @Inject
    public DeviceService(
        Logger log, 
        ICreateDeviceUseCase createDeviceUseCase,
        ICheckMqttUsernameExistsUseCase checkMqttUsernameExistsUseCase,
        IGetDeviceByUuidUseCase getDeviceByUuidUseCase,
        IGetAllDevicesUseCase getAllDevicesUseCase
    ){
        this.log = log;
        this.createDeviceUseCase = createDeviceUseCase;
        this.checkMqttUsernameExistsUseCase = checkMqttUsernameExistsUseCase;
        this.getDeviceByUuidUseCase = getDeviceByUuidUseCase;
        this.getAllDevicesUseCase = getAllDevicesUseCase;
    }

    public Uni<Response> createDevice(CreateDeviceRequest request){
        log.infof("Creating new device {}", request.getDeviceName());
        if (request.getMqttUsername() != null && request.getMqttUsername().toLowerCase().contains("admin")) {
            log.warnf("Attempted to create device with invalid MQTT username (contains 'admin'): %s", request.getMqttUsername());
            return Uni.createFrom().failure(new InvalidMqttUsernameException("MQTT username cannot contain 'admin'"));
        }
        return checkMqttUsernameExistsUseCase.execute(request.getMqttUsername()).chain(exists ->{
            if(exists){
                log.warnf("Attempted to create device with existing MQTT username: %s", request.getMqttUsername());
                return Uni.createFrom().failure(new MqttUsernameAlreadyExistsException("Mqtt username already exists"));
            }
            return createDeviceUseCase.execute(request.getDeviceName(), request.getMqttUsername())
            .onItem().transform(
                response ->{
                    log.info("Device created successfully");
                    return Response.ok().entity(response).build();
                }
            );
        });
    }

    public Uni<Response> getDeviceInfoByUuid(String uuid){
        return getDeviceByUuidUseCase.execute(uuid).onItem().transform(
            deviceInfo -> Response.ok().entity(deviceInfo).build()
        );
    }

    /**
     * Get all devices with pagination support
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Response containing paged device list
     */
    public Uni<Response> getAllDevices(int page, int size) {
        log.infof("Getting all devices with pagination - page: %d, size: %d", page, size);
        return getAllDevicesUseCase.execute(page, size)
            .onItem().transform(pagedResponse -> 
                Response.ok().entity(pagedResponse).build()
            );
    }
}
