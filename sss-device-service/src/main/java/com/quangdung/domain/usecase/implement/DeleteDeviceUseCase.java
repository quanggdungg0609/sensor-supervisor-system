package com.quangdung.domain.usecase.implement;

import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.domain.usecase.interfaces.IDeleteDeviceUseCase;
import com.quangdung.infrastructure.dao.mqtt_dao.MqttDAO;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of the device deletion use case
 */
@ApplicationScoped
public class DeleteDeviceUseCase implements IDeleteDeviceUseCase {
    private final Logger log;
    private final IDeviceRepository deviceRepository;
    private final MqttDAO mqttDAO;
    
    @Inject
    public DeleteDeviceUseCase(
        Logger log,
        IDeviceRepository deviceRepository,
        @RestClient MqttDAO mqttDAO
    ) {
        this.log = log;
        this.deviceRepository = deviceRepository;
        this.mqttDAO = mqttDAO;
    }
    
    /**
     * Deletes a device by its UUID and its associated MQTT account
     * 
     * @param deviceUuid The UUID of the device to delete
     * @return Boolean indicating success or failure of the deletion
     */
    @Override
    public Uni<Boolean> execute(String deviceUuid) {
        log.infof("Executing delete device use case for UUID: %s", deviceUuid);
        
        // First delete the MQTT account
        return mqttDAO.deleteMqttAccountByDeviceUuid(deviceUuid)
            .onItem().transformToUni(response -> {
                log.infof("MQTT account deleted for device UUID: %s", deviceUuid);
                
                // Then delete the device from the database
                return deviceRepository.deleteDeviceByUuid(deviceUuid)
                    .onItem().transform(deleted -> {
                        if (deleted) {
                            log.infof("Device with UUID %s deleted successfully", deviceUuid);
                        } else {
                            log.warnf("Failed to delete device with UUID %s", deviceUuid);
                        }
                        return deleted;
                    });
            })
            .onFailure().recoverWithItem(throwable -> {
                log.errorf(throwable, "Error deleting MQTT account for device UUID %s", deviceUuid);
                return false;
            });
    }
}