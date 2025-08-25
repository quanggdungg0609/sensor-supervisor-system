package org.quangdung.domain.repository;

import org.quangdung.domain.entity.DeviceInfo;
import org.quangdung.domain.entity.MqttAccount;

import io.smallrye.mutiny.Uni;

public interface IMqttRepository {
    Uni<Boolean> checkMqttUsernameExists(String mqttUsername);
    Uni<Boolean> checkClientIdExists(String clientId);
    Uni<MqttAccount> createAccount(MqttAccount mqttAccount);
    Uni<MqttAccount> findByMqttUsername(String mqttUsername);
    Uni<MqttAccount> findByClientId(String clientId);
    Uni<MqttAccount> findByDeviceUuid(String deviceUuid);
    Uni<DeviceInfo> getDeviceInfoByClientId(String clientId);
    Uni<String> getMqttUsernameByClientId(String clientId);
    
    /**
     * Updates the password for an MQTT account identified by clientId
     * 
     * @param clientId The client ID to update password for
     * @param hashedPassword The new hashed password
     * @return Uni<Boolean> indicating success or failure
     */
    Uni<Boolean> updatePassword(String clientId, String hashedPassword);
    
    /**
     * Updates the password for an MQTT account identified by device UUID
     * 
     * @param deviceUuid The device UUID to update password for
     * @param hashedPassword The new hashed password
     * @return Uni<Boolean> indicating success or failure
     */
    Uni<Boolean> updatePasswordByDeviceUuid(String deviceUuid, String hashedPassword);
}
