package org.quangdung.domain.use_case.interfaces;

import io.smallrye.mutiny.Uni;

/**
 * Use case interface for deleting MQTT account by device UUID
 */
public interface IDeleteAccountByUuidUseCase {
    /**
     * Deletes an MQTT account identified by device UUID
     * 
     * @param deviceUuid The device UUID of the account to delete
     * @return Uni<Boolean> indicating success or failure of the deletion
     * @throws org.quangdung.core.exception.MqttAccountNotExistsException if device UUID doesn't exist
     */
    Uni<Boolean> execute(String deviceUuid);
}