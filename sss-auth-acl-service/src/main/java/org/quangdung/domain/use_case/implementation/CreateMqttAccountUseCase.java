package org.quangdung.domain.use_case.implementation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.quangdung.core.exception.MqttUsernameAlreadyExistsException;
import org.quangdung.core.utils.password_util.IPasswordUtil;
import org.quangdung.core.utils.uid_util.IUidUtil;
import org.quangdung.domain.entity.MqttAccount;
import org.quangdung.domain.entity.MqttPermission;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IMqttCreateAccountUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CreateMqttAccountUseCase  implements IMqttCreateAccountUseCase{
    private IUidUtil uidUtil;
    private IPasswordUtil passwordUtil;
    private IMqttRepository mqttRepository;

    @Inject
    public CreateMqttAccountUseCase(IUidUtil uidUtil, IPasswordUtil passwordUtil, IMqttRepository mqttRepository) {
        this.uidUtil = uidUtil;
        this.passwordUtil = passwordUtil;
        this.mqttRepository = mqttRepository;

    }


    /**
     * Executes the device creation process.
     * 
     * This method performs the following operations:
     * 1. Validates that the MQTT username is not already in use
     * 2. Generates a unique device UID and secure password with retry mechanism
     * 3. Creates MQTT account with hashed password and default permissions
     * 5. Persists the device to the repository
     * 6. Returns the MQTT account
     *
     * @param username the MQTT username for the device's MQTT account
     * @param deviceUid the unique device UID linked with account
     * @return Uni<MqttAccount> a reactive stream containing the created mqtt account with raw password
     * @throws MqttAccountAlreadyExistsException if the MQTT username already exists in the system
     */
    @Override
    public Uni<MqttAccount> execute(String username, String deviceUuid) {
        return mqttRepository.checkMqttUsernameExists(username).onItem().transformToUni(exists -> {
            if (exists){
                return Uni.createFrom().failure(new MqttUsernameAlreadyExistsException("Mqtt username already exists: "+ username));
            }
            
           return generateUniqueClientId(0).onItem().transformToUni(clientId -> {
                String rawPassword = passwordUtil.generatePassword(8);
                String hashedPassword = passwordUtil.hash(rawPassword);
        
                // Create default MQTT permissions for the device
                List<MqttPermission> defaultPermissions = createDefaultPermissions(clientId);

                MqttAccount account = MqttAccount.builder()
                    .mqttUsername(username)
                    .mqttPassword(hashedPassword)
                    .deviceUuid(deviceUuid)
                    .clientId(clientId)
                    .permissions(defaultPermissions)
                    .build();
                return mqttRepository.createAccount(account).onItem().transform(createdAccount -> {
                    createdAccount.setMqttPassword(rawPassword);
                    return createdAccount;
                });
            });
        });
    }
    
    
    private Uni<String> generateUniqueClientId(int attempt) {
        if (attempt >= 5) {
            return Uni.createFrom().failure(new RuntimeException("Unable to generate unique client ID after 5 attempts"));
        }
        
        String deviceUid = uidUtil.generateUid();
        
        return mqttRepository.checkClientIdExists(deviceUid).onItem().transformToUni(exists -> {
            if (exists) {
                // Client ID already exists, retry with next attempt
                return generateUniqueClientId(attempt + 1);
            } else {
                // Client ID is unique, return it
                return Uni.createFrom().item(deviceUid);
            }
        });
    }
    
    /**
     * Creates default MQTT permissions for a new device.
     * 
     * Default permissions include:
     * - sensor/[clientId]/telemetry (PUBLISH) - for sending sensor data with all QoS levels
     * - sensor/[clientId]/status (PUBLISH) - for LWT (Last Will Testament) messages with all QoS levels
     * - sensor/[clientId]/command (SUBSCRIBE) - for receiving commands from server with all QoS levels
     * 
     * All permissions allow QoS levels 0, 1, and 2 by default.
     *
     * @param clientId the unique client ID for the device
     * @return List of default MQTT permissions with QoS level configurations
     */
    private List<MqttPermission> createDefaultPermissions(String clientId) {
        List<MqttPermission> permissions = new ArrayList<>();
        
        // Default QoS levels: 0 (At most once), 1 (At least once), 2 (Exactly once)
        List<Integer> defaultQosLevels = Arrays.asList(0, 1, 2);
        
        // Permission for telemetry topic (PUBLISH)
        permissions.add(MqttPermission.builder()
            .topicPattern("sensors/" + clientId + "/telemetry")
            .action(MqttPermission.MqttAction.PUBLISH)
            .permission(MqttPermission.Permission.ALLOW)
            .allowedQosLevels(defaultQosLevels)
            .build());
        
        // Permission for status topic (PUBLISH) - for LWT messages
        permissions.add(MqttPermission.builder()
            .topicPattern("sensors/" + clientId + "/status")
            .action(MqttPermission.MqttAction.PUBLISH)
            .permission(MqttPermission.Permission.ALLOW)
            .allowedQosLevels(defaultQosLevels)
            .build());

        permissions.add(
            MqttPermission.builder()
                .topicPattern("sensors/" + clientId + "/power_outage")
                .action(MqttPermission.MqttAction.PUBLISH)
                .permission(MqttPermission.Permission.ALLOW)
                .allowedQosLevels(defaultQosLevels)
                .build()
        );
        
        // Permission for command topic (SUBSCRIBE)
        permissions.add(MqttPermission.builder()
            .topicPattern("sensors/" + clientId + "/command")
            .action(MqttPermission.MqttAction.SUBSCRIBE)
            .permission(MqttPermission.Permission.ALLOW)
            .allowedQosLevels(defaultQosLevels)
            .build());
        
        return permissions;
    }
}
