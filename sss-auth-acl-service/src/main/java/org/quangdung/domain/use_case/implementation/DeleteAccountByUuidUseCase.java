package org.quangdung.domain.use_case.implementation;

import org.jboss.logging.Logger;
import org.quangdung.core.exception.MqttAccountNotExistsException;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IDeleteAccountByUuidUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case implementation for deleting MQTT account by device UUID
 */
@ApplicationScoped
public class DeleteAccountByUuidUseCase implements IDeleteAccountByUuidUseCase {
    private final Logger log;
    private final IMqttRepository mqttRepository;
    
    @Inject
    public DeleteAccountByUuidUseCase(
        Logger log,
        IMqttRepository mqttRepository
    ) {
        this.log = log;
        this.mqttRepository = mqttRepository;
    }

    /**
     * Deletes an MQTT account identified by device UUID
     * 
     * @param deviceUuid The device UUID of the account to delete
     * @return Uni<Boolean> indicating success or failure of the deletion
     * @throws MqttAccountNotExistsException if device UUID doesn't exist
     */
    @Override
    public Uni<Boolean> execute(String deviceUuid) {
        log.info("Processing delete account request for deviceUuid: " + deviceUuid);
        
        // First check if device UUID exists
        return mqttRepository.findByDeviceUuid(deviceUuid)
            .onItem().transformToUni(mqttAccount -> {
                if (mqttAccount == null) {
                    log.warn("Device UUID not found: " + deviceUuid);
                    return Uni.createFrom().failure(
                        new MqttAccountNotExistsException("Device with UUID " + deviceUuid + " does not exist")
                    );
                }
                
                // Delete account from database
                return mqttRepository.deleteByDeviceUuid(deviceUuid)
                    .onItem().transform(success -> {
                        if (success) {
                            log.info("Account deleted successfully for deviceUuid: " + deviceUuid);
                            return true;
                        } else {
                            throw new RuntimeException("Failed to delete account for deviceUuid: " + deviceUuid);
                        }
                    });
            });
    }
}