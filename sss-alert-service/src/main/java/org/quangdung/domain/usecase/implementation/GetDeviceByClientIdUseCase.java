package org.quangdung.domain.usecase.implementation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quangdung.domain.model.DeviceInfoModel;
import org.quangdung.domain.usecase.interfaces.IGetDeviceByClientIdUseCase;
import org.quangdung.infrastructure.component.device_rest_client_component.DeviceRestClientComponent;
import org.quangdung.infrastructure.entity.DeviceInfoEntity;

import io.smallrye.mutiny.Uni;

@ApplicationScoped
public class GetDeviceByClientIdUseCase implements IGetDeviceByClientIdUseCase {

    private static final Logger LOG = Logger.getLogger(GetDeviceByClientIdUseCase.class);
    
    @Inject
    @RestClient
    DeviceRestClientComponent deviceRestClient;
    
    /**
     * Retrieves device information by client ID from the device service
     * 
     * @param clientId The client ID of the device to retrieve information for
     * @return Uni<DeviceInfoModel> containing the device information
     */
    @Override
    public Uni<DeviceInfoModel> execute(String clientId) {
        LOG.infof("Retrieving device info for clientId: %s", clientId);
        
        return deviceRestClient.getDeviceByClientId(clientId)
                .onItem().transform(this::mapToDeviceInfoModel)
                .onFailure().recoverWithItem(e -> {
                    LOG.errorf(e, "Failed to retrieve device info for clientId: %s", clientId);
                    
                    // Handle 404 error from REST client
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("404") || 
                         e.getMessage().contains("Not Found") ||
                         e.getMessage().contains("Not Found, status code 404"))) {
                        throw new jakarta.ws.rs.NotFoundException("Device not found: " + clientId);
                    }
                    
                    // Handle 500 error from REST client
                    if (e.getMessage() != null && 
                        (e.getMessage().contains("500") || 
                         e.getMessage().contains("Internal Server Error"))) {
                        throw new jakarta.ws.rs.InternalServerErrorException("Device service is currently unavailable");
                    }
                    
                    // Handle other errors
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