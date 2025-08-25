package org.quangdung.domain.use_case.interfaces;

import io.smallrye.mutiny.Uni;

/**
 * Use case interface for changing MQTT account password
 */
public interface IChangePasswordUseCase {
    /**
     * Changes the password for a device identified by device UUID
     * 
     * @param deviceUuid The device UUID to change password for
     * @return Uni containing the new unhashed password
     * @throws org.quangdung.core.exception.MqttAccountNotExistsException if device UUID doesn't exist
     */
    Uni<String> execute(String deviceUuid);
}