package org.quangdung.domain.usecase.implementation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quangdung.domain.model.DeviceInfoModel;
import org.quangdung.domain.usecase.interfaces.IGetDeviceByUuidUseCase;
import org.quangdung.infrastructure.component.device_rest_client_component.DeviceRestClientComponent;
import org.quangdung.infrastructure.entity.DeviceInfoEntity;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class GetDeviceByUuidUseCase implements IGetDeviceByUuidUseCase {

    private static final Logger LOG = Logger.getLogger(GetDeviceByUuidUseCase.class);
    
    @Inject
    @RestClient
    DeviceRestClientComponent deviceRestClient;
    
    /**
     * Retrieves device information by device UUID from the device service
     * 
     * @param deviceUuid The device UUID to retrieve information for
     * @return Uni<DeviceInfoModel> containing the device information
     */
    @Override
    public Uni<DeviceInfoModel> execute(String deviceUuid) {
        LOG.infof("Retrieving device info for deviceUuid: %s", deviceUuid);
        
        return deviceRestClient.getDeviceByUuid(deviceUuid)
                .onItem().transform(this::mapToDeviceInfoModel)
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf(e, "Failed to retrieve device info for deviceUuid: %s", deviceUuid);
                    
                    // Handle ClientWebApplicationException first (more specific)
                    if (e instanceof org.jboss.resteasy.reactive.ClientWebApplicationException) {
                        LOG.error("ClientWebApplicationException occurred", e);
                        
                        // Check for specific status codes within ClientWebApplicationException
                        org.jboss.resteasy.reactive.ClientWebApplicationException webEx = 
                            (org.jboss.resteasy.reactive.ClientWebApplicationException) e;
                        
                        try {
                            int status = webEx.getResponse().getStatus();
                            
                            if (status == 404) {
                                throw new jakarta.ws.rs.NotFoundException("Device not found: " + deviceUuid);
                            } else if (status == 500) {
                                // Implement fallback strategy or retry logic here if needed
                                throw new jakarta.ws.rs.InternalServerErrorException("Device service returned 500 error");
                            } else {
                                throw new jakarta.ws.rs.InternalServerErrorException("Device service returned error code: " + status);
                            }
                        } catch (Exception statusEx) {
                            // If we can't get the status for some reason, handle as a connection issue
                            throw new jakarta.ws.rs.InternalServerErrorException("Connection issue with device service: " + e.getMessage());
                        }
                    }
                    
                    // Handle other exceptions with message checks (less specific)
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("404") || 
                         e.getMessage().contains("Not Found") ||
                         e.getMessage().contains("Not Found, status code 404"))) {
                        LOG.error(e);
                        throw new jakarta.ws.rs.NotFoundException("Device not found: " + deviceUuid);
                    }
                    
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("500") || 
                         e.getMessage().contains("Internal Server Error"))) {
                        LOG.error(e);
                        throw new jakarta.ws.rs.InternalServerErrorException("Device service is currently unavailable");
                    }
                    
                    throw new RuntimeException("Failed to retrieve device info: " + e.getMessage());
                });
    }
    
    /**
     * Maps DeviceInfoEntity to DeviceInfoModel
     * 
     * @param entity The DeviceInfoEntity to map
     * @return DeviceInfoModel mapped from the entity
     */
    private DeviceInfoModel mapToDeviceInfoModel(DeviceInfoEntity entity) {
        return DeviceInfoModel.builder()
                .clientId(entity.getClientId())
                .deviceUuid(entity.getDeviceUuid())
                .mqttUsername(entity.getMqttUsername())
                .deviceName(entity.getDeviceName())
                .build();
    }
}