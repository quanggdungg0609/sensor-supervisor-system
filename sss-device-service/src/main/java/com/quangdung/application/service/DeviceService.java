package com.quangdung.application.service;

import org.jboss.logging.Logger;

import com.quangdung.application.dto.request.CreateDeviceRequest;
import com.quangdung.core.exception.DeviceNotFoundException;
import com.quangdung.core.exception.InvalidMqttUsernameException;
import com.quangdung.core.exception.MqttUsernameAlreadyExistsException;
import com.quangdung.domain.usecase.interfaces.ICheckMqttUsernameExistsUseCase;
import com.quangdung.domain.usecase.interfaces.ICreateDeviceUseCase;
import com.quangdung.domain.usecase.interfaces.IDeleteDeviceUseCase;
import com.quangdung.domain.usecase.interfaces.IGetDeviceByUuidUseCase;
import com.quangdung.domain.usecase.interfaces.IGetAllDevicesUseCase;
import com.quangdung.domain.usecase.interfaces.IGetDeviceByClientIdUseCase;

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
    private final IDeleteDeviceUseCase deleteDeviceUseCase;
    private final IGetDeviceByClientIdUseCase getDeviceByClientIdUseCase;

    @Inject
    public DeviceService(
        Logger log, 
        ICreateDeviceUseCase createDeviceUseCase,
        ICheckMqttUsernameExistsUseCase checkMqttUsernameExistsUseCase,
        IGetDeviceByUuidUseCase getDeviceByUuidUseCase,
        IGetAllDevicesUseCase getAllDevicesUseCase,
        IDeleteDeviceUseCase deleteDeviceUseCase,
        IGetDeviceByClientIdUseCase getDeviceByClientIdUseCase
    ){
        this.log = log;
        this.createDeviceUseCase = createDeviceUseCase;
        this.checkMqttUsernameExistsUseCase = checkMqttUsernameExistsUseCase;
        this.getDeviceByUuidUseCase = getDeviceByUuidUseCase;
        this.getAllDevicesUseCase = getAllDevicesUseCase;
        this.deleteDeviceUseCase = deleteDeviceUseCase;
        this.getDeviceByClientIdUseCase = getDeviceByClientIdUseCase;
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
    
    public Uni<Response> getDeviceInfoByUuid(String deviceUuid){
        return getDeviceByUuidUseCase.execute(deviceUuid)
            .onItem().transform(deviceInfo -> Response.ok(deviceInfo).build());
    }
    
    /**
     * Get all devices with pagination
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Response containing paged device list
     */
    public Uni<Response> getAllDevices(int page, int size) {
        return getAllDevicesUseCase.execute(page, size)
            .onItem().transform(pagedResponse -> Response.ok(pagedResponse).build());
    }
    
    /**
     * Delete a device by its UUID
     * @param deviceUuid UUID of the device to delete
     * @return Response indicating success or failure
     */
    public Uni<Response> deleteDevice(String deviceUuid) {
        log.infof("Deleting device with UUID: %s", deviceUuid);
        return deleteDeviceUseCase.execute(deviceUuid)
            .onItem().transform(deleted -> {
                if (deleted) {
                    return Response.ok()
                        .entity(java.util.Map.of(
                            "message", "Device deleted successfully",
                            "device_uuid", deviceUuid
                        ))
                        .build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(java.util.Map.of(
                            "error", "Device not found or could not be deleted",
                            "device_uuid", deviceUuid
                        ))
                        .build();
                }
            })
            .onFailure().recoverWithItem(throwable -> {
                log.errorf(throwable, "Error deleting device with UUID %s", deviceUuid);
                return Response.serverError()
                    .entity(java.util.Map.of(
                        "error", "Failed to delete device: " + throwable.getMessage(),
                        "device_uuid", deviceUuid
                    ))
                    .build();
            });
    }
    
    /**
     * Get device information by client ID
     * @param clientId Client ID of the device
     * @return Response containing device information or error
     */
    public Uni<Response> getDeviceByClientId(String clientId) {
        return getDeviceByClientIdUseCase.execute(clientId)
            .onItem().transform(deviceInfo -> Response.ok(deviceInfo).build())
            .onFailure().recoverWithItem(throwable -> {
                log.errorf(throwable, "Error getting device with client ID %s", clientId);
                if (throwable instanceof DeviceNotFoundException) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(java.util.Map.of(
                            "error", "Device not found",
                            "client_id", clientId
                        ))
                        .build();
                }
                return Response.serverError()
                    .entity(java.util.Map.of(
                        "error", "Failed to get device: " + throwable.getMessage(),
                        "client_id", clientId
                    ))
                    .build();
            });
    }
}
