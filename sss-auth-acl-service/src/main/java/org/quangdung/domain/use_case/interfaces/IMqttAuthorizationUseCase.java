package org.quangdung.domain.use_case.interfaces;

import io.smallrye.mutiny.Uni;

public interface IMqttAuthorizationUseCase {
    /**
     * Authorizes an MQTT operation for a specific topic
     * @param username The MQTT username (mqttId)
     * @param clientId The client ID
     * @param topic The MQTT topic to check
     * @param action The action to perform ("publish" or "subscribe")
     * @param qosLevel The QoS level (0, 1, or 2)
     * @return Uni<Boolean> true if operation is authorized, false otherwise
     */
    Uni<Boolean> authorize(String username, String clientId, String topic, String action, int qosLevel);
}
