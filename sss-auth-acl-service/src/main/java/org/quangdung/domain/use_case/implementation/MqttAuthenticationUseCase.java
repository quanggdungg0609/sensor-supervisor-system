package org.quangdung.domain.use_case.implementation;

import org.jboss.logging.Logger;
import org.quangdung.core.utils.password_util.IPasswordUtil;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IMqttAuthenticationUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttAuthenticationUseCase implements IMqttAuthenticationUseCase {
    private final IMqttRepository mqttRepository;
    private final IPasswordUtil passwordUtil;
    private final Logger log;
    
    /**
     * Constructor with dependency injection
     * @param mqttRepository Repository for MQTT account operations
     * @param passwordUtil Utility for password verification
     * @param log Logger for debugging and monitoring
     */
    @Inject
    public MqttAuthenticationUseCase(
        IMqttRepository mqttRepository,
        IPasswordUtil passwordUtil,
        Logger log
    ) {
        this.mqttRepository = mqttRepository;
        this.passwordUtil = passwordUtil;
        this.log = log;
    }
    
    /**
     * Authenticates an MQTT client using username and password
     * @param username The MQTT username (mqttId)
     * @param password The plain text password to verify
     * @param clientId The client ID for additional validation
     * @return Uni<Boolean> true if authentication successful, false otherwise
     */
    @Override
    public Uni<Boolean> authenticate(String username, String password, String clientId) {        
        // Validate input parameters
        if (username == null || username.trim().isEmpty() ||
            password == null || password.trim().isEmpty()) {
            log.warn("Authentication failed: Invalid username or password");
            return Uni.createFrom().item(false);
        }
        
        // Find MQTT account by username
        return mqttRepository.findByMqttUsername(username)
            .onItem().transform(mqttAccount -> {
                if (mqttAccount == null) {
                    log.warn("Authentication failed: MQTT account not found for username: " + username);
                    return false;
                }
                
                // Verify password
                boolean passwordMatch = passwordUtil.isMatch(password, mqttAccount.getMqttPassword());
                if (!passwordMatch) {
                    log.warn("Authentication failed: Invalid password for username: " + username);
                    return false;
                }
                
                // Optional: Verify client ID matches (if provided)
                if (clientId != null && !clientId.trim().isEmpty()) {
                    if (!clientId.equals(mqttAccount.getClientId())) {
                        log.warn("Authentication failed: Client ID mismatch for username: " + username);
                        return false;
                    }
                }
                
                log.info("Authentication successful for username: " + username);
                return true;
            })
            .onFailure().transform(throwable -> {
                log.error("Authentication error for username: " + username, throwable);
                return new RuntimeException("Authentication failed due to system error", throwable);
            });
    }
}
