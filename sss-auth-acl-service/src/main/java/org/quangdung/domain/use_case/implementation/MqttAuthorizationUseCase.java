package org.quangdung.domain.use_case.implementation;

import org.jboss.logging.Logger;
import org.quangdung.domain.entity.MqttPermission;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IMqttAuthorizationUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttAuthorizationUseCase implements IMqttAuthorizationUseCase {
    
    private final IMqttRepository mqttRepository;
    private final Logger log;
    
    /**
     * Constructor with dependency injection
     * @param mqttRepository Repository for MQTT account operations
     * @param log Logger for debugging and monitoring
     */
    @Inject
    public MqttAuthorizationUseCase(
        IMqttRepository mqttRepository,
        Logger log
    ) {
        this.mqttRepository = mqttRepository;
        this.log = log;
    }
    
    /**
     * Authorizes an MQTT operation for a specific topic
     * @param username The MQTT username (mqttId)
     * @param clientId The client ID
     * @param topic The MQTT topic to check
     * @param action The action to perform ("publish" or "subscribe")
     * @param qosLevel The QoS level (0, 1, or 2)
     * @return Uni<Boolean> true if operation is authorized, false otherwise
     */
    @Override
    public Uni<Boolean> authorize(String username, String clientId, String topic, String action, int qosLevel) {
        // Validate input parameters
        if (username == null || username.trim().isEmpty() ||
            topic == null || topic.trim().isEmpty() ||
            action == null || action.trim().isEmpty()) {
            log.warn("Authorization failed: Invalid parameters");
            return Uni.createFrom().item(false);
        }
        
        // Convert action string to MqttAction enum
        MqttPermission.MqttAction mqttAction = convertActionToEnum(action);
        if (mqttAction == null) {
            log.warn("Authorization failed: Invalid action: " + action);
            return Uni.createFrom().item(false);
        }
        
        // Find MQTT account by username
        return mqttRepository.findByMqttUsername(username)
            .onItem().transform(mqttAccount -> {
                if (mqttAccount == null) {
                    log.warn("Authorization failed: MQTT account not found for username: " + username);
                    return false;
                }
                
                // Optional: Verify client ID matches
                if (clientId != null && !clientId.trim().isEmpty()) {
                    if (!clientId.equals(mqttAccount.getClientId())) {
                        log.warn("Authorization failed: Client ID mismatch for username: " + username);
                        return false;
                    }
                }
                
                // Check permissions
                boolean hasPermission = mqttAccount.hasPermission(topic, mqttAction, qosLevel);
                
                if (hasPermission) {
                    log.info("Authorization successful for username: " + username + 
                            ", topic: " + topic + ", action: " + action);
                } else {
                    log.warn("Authorization failed: No permission for username: " + username + 
                            ", topic: " + topic + ", action: " + action);
                }
                
                return hasPermission;
            })
            .onFailure().transform(throwable -> {
                log.error("Authorization error for username: " + username, throwable);
                return new RuntimeException("Authorization failed due to system error", throwable);
            });
    }
    
    /**
     * Converts action string to MqttAction enum
     * @param action The action string from EMQX ("publish" or "subscribe")
     * @return MqttAction enum or null if invalid
     */
    private MqttPermission.MqttAction convertActionToEnum(String action) {
        if (action == null) {
            return null;
        }
        
        switch (action.toLowerCase()) {
            case "publish":
                return MqttPermission.MqttAction.PUBLISH;
            case "subscribe":
                return MqttPermission.MqttAction.SUBSCRIBE;
            default:
                return null;
        }
    }
}
