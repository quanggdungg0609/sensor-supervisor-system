package org.quangdung.domain.entity;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.ArrayList;


import org.quangdung.infrastructure.entity.MqttAccountEntity;
import org.quangdung.infrastructure.entity.MqttPermissionEntity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttAccount {
    private Long id;

    private String mqttUsername;

    private String mqttPassword;

    private String clientId;

    private String deviceUuid;
    
    @Builder.Default
    private List<MqttPermission> permissions = new ArrayList<>();

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;



    public static MqttAccount fromEntity(MqttAccountEntity entity) {
        if (entity == null) {
            return null;
        }
        
        List<MqttPermission> permissions = null;
        if (entity.getPermissions() != null) {
            permissions = entity.getPermissions().stream()
                .map(MqttPermission::fromEntity)
                .collect(Collectors.toList());
        }
        
        return MqttAccount.builder()
            .id(entity.id)
            .deviceUuid(entity.getDeivceUuid())
            .mqttUsername(entity.getMqttUsername())
            .mqttPassword(entity.getMqttPassword())
            .clientId(entity.getClientId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .permissions(permissions)
            .build();
    }

    public MqttAccountEntity toEntity() {
        List<MqttPermissionEntity> permissionEntities = null;
        if (this.permissions != null) {
            permissionEntities = this.permissions.stream()
                .map(MqttPermission::toEntity)
                .collect(Collectors.toList());
        }
        
        MqttAccountEntity entity = MqttAccountEntity.builder()
            .mqttUsername(this.mqttUsername)
            .mqttPassword(this.mqttPassword)
            .deivceUuid(this.deviceUuid)
            .clientId(this.clientId)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .permissions(permissionEntities)
            .build();
            
        if (this.id != null) {
            entity.id = this.id;
        }
        
        // Set bidirectional relationship
        if (permissionEntities != null) {
            permissionEntities.forEach(p -> p.setMqttAccount(entity));
        }
        
        return entity;
    }

    public void addPermission(MqttPermission permission) {
        if (this.permissions == null) {
            this.permissions = new ArrayList<>();
        }
        this.permissions.add(permission);
    }
    
    /**
     * Removes a permission from this MQTT account
     * @param permission The permission to remove
     */
    public void removePermission(MqttPermission permission) {
        if (this.permissions != null) {
            this.permissions.remove(permission);
        }
    }
    
    /**
     * Checks if this account has a specific permission for a topic, action, and QoS level
     * @param topic The MQTT topic to check
     * @param action The MQTT action to check
     * @param qosLevel The QoS level to check (0, 1, or 2)
     * @return true if permission is granted for the specified QoS level, false otherwise
     */
    public boolean hasPermission(String topic, MqttPermission.MqttAction action, int qosLevel) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }
        
        return permissions.stream()
            .filter(p -> p.getPermission() == MqttPermission.Permission.ALLOW)
            .anyMatch(p -> p.matchesTopic(topic) && 
                     (p.getAction() == action || p.getAction() == MqttPermission.MqttAction.ALL) &&
                     p.isQosLevelAllowed(qosLevel));
    }
}
