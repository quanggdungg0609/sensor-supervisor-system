package org.quangdung.domain.use_case.interfaces;

import io.smallrye.mutiny.Uni;

public interface IMqttAuthenticationUseCase {
    /**
     * Authenticates an MQTT client using username and password
     * @param username The MQTT username (mqttId)
     * @param password The plain text password to verify
     * @param clientId The client ID for additional validation
     * @return Uni<Boolean> true if authentication successful, false otherwise
     */
    Uni<Boolean> authenticate(String username, String password, String clientId);
}
