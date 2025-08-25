package org.quangdung.domain.use_case.implementation;

import org.jboss.logging.Logger;
import org.quangdung.core.exception.MqttAccountNotExistsException;
import org.quangdung.core.utils.password_util.IPasswordUtil;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IChangePasswordUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Use case implementation for changing MQTT account password
 */
@ApplicationScoped
public class ChangePasswordUseCase implements IChangePasswordUseCase {
    private final Logger log;
    private final IMqttRepository mqttRepository;
    private final IPasswordUtil passwordUtil;
    
    @Inject
    public ChangePasswordUseCase(
        Logger log,
        IMqttRepository mqttRepository,
        IPasswordUtil passwordUtil
    ) {
        this.log = log;
        this.mqttRepository = mqttRepository;
        this.passwordUtil = passwordUtil;
    }
    
    
    /**
     * Changes the password for a device identified by device UUID
     * 
     * @param deviceUuid The device UUID to change password for
     * @return Uni containing the new unhashed password
     * @throws MqttAccountNotExistsException if device UUID doesn't exist
     */
    @Override
    public Uni<String> execute(String deviceUuid) {
        log.info("Processing change password request for deviceUuid: " + deviceUuid);
        
        // First check if device UUID exists
        return mqttRepository.findByDeviceUuid(deviceUuid)
            .onItem().transformToUni(mqttAccount -> {
                if (mqttAccount == null) {
                    log.warn("Device UUID not found: " + deviceUuid);
                    return Uni.createFrom().failure(
                        new MqttAccountNotExistsException("Device with UUID " + deviceUuid + " does not exist")
                    );
                }
                
                // Generate new password (8 characters - same as create account)
                String newPassword = passwordUtil.generatePassword(8);
                log.info("Generated new password for deviceUuid: " + deviceUuid);
                
                // Hash the new password
                String hashedPassword = passwordUtil.hash(newPassword);
                
                // Update password in database
                return mqttRepository.updatePasswordByDeviceUuid(deviceUuid, hashedPassword)
                    .onItem().transform(success -> {
                        if (success) {
                            log.info("Password updated successfully for deviceUuid: " + deviceUuid);
                            return newPassword; // Return unhashed password
                        } else {
                            throw new RuntimeException("Failed to update password for deviceUuid: " + deviceUuid);
                        }
                    });
            });
    }
}