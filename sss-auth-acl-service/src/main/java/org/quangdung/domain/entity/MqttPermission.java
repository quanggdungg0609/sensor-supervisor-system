package org.quangdung.domain.entity;

import java.util.Arrays;
import java.time.LocalDateTime;
import java.util.List;

import org.quangdung.infrastructure.entity.MqttPermissionEntity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MqttPermission {
    private Long id;
    private Long mqttAccountId;
    private String topicPattern;
    private MqttAction action;  
    private Permission permission;

    @Builder.Default
    private List<Integer> allowedQosLevels = Arrays.asList(0, 1, 2);

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    public enum MqttAction {
        PUBLISH,
        SUBSCRIBE,
        ALL
    }

    public enum Permission {
        ALLOW,
        DENY,
        ALL
    }

    public static MqttPermission fromEntity(MqttPermissionEntity entity) {
        if (entity == null) {
            return null;
        }
        
        return MqttPermission.builder()
            .id(entity.id)
            .mqttAccountId(entity.getMqttAccount() != null ? entity.getMqttAccount().id : null)
            .topicPattern(entity.getTopicPattern())
            .action(mapActionFromEntity(entity.getAction()))
            .permission(mapPermissionFromEntity(entity.getPermission()))
            .allowedQosLevels(entity.getAllowedQosLevels())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .build();
    }

    public boolean isQosLevelAllowed(int qosLevel) {
        return allowedQosLevels != null && allowedQosLevels.contains(qosLevel);
    }

    public boolean matchesTopic(String topic) {
        if (topicPattern == null || topic == null) {
            return false;
        }
        
        // Exact match
        if (topicPattern.equals(topic)) {
            return true;
        }
        
        // Convert MQTT wildcards to regex
        String regex = topicPattern
            .replace("+", "[^/]+")  // + matches single level
            .replace("#", ".*");     // # matches multiple levels
        
        return topic.matches(regex);
    }

    private static Permission mapPermissionFromEntity(MqttPermissionEntity.MqttPermission entityPermission) {
        if (entityPermission == null) {
            return null;
        }
        switch (entityPermission) {
            case ALLOW:
                return Permission.ALLOW;
            case DENY:
                return Permission.DENY;

            default:
                throw new IllegalArgumentException("Unknown Permission: " + entityPermission);
        }
    }

    public MqttPermissionEntity toEntity() {
        MqttPermissionEntity entity = MqttPermissionEntity.builder()
            .topicPattern(this.topicPattern)
            .action(mapActionToEntity(this.action))
            .permission(mapPermissionToEntity(this.permission))
            .allowedQosLevels(this.allowedQosLevels)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .build();
            
        if (this.id != null) {
            entity.id = this.id;
        }
        
        return entity;
    }

    public void updateEntity(MqttPermissionEntity entity) {
        if (entity == null) {
            return;
        }
        entity.setTopicPattern(this.topicPattern);
        entity.setAction(mapActionToEntity(this.action));
        entity.setPermission(mapPermissionToEntity(this.permission));
        entity.setAllowedQosLevels(this.allowedQosLevels);
    }

     private static MqttPermissionEntity.MqttPermission mapPermissionToEntity(Permission domainPermission) {
        if (domainPermission == null) {
            return null;
        }
        
        switch (domainPermission) {
            case ALLOW:
                return MqttPermissionEntity.MqttPermission.ALLOW;
            case DENY:
                return MqttPermissionEntity.MqttPermission.DENY;
            default:
                throw new IllegalArgumentException("Unknown Permission: " + domainPermission);
        }
    }

    private static MqttAction mapActionFromEntity(MqttPermissionEntity.MqttAction entityAction) {
        if (entityAction == null) {
            return null;
        }
        
        switch (entityAction) {
            case PUBLISH:
                return MqttAction.PUBLISH;
            case SUBSCRIBE:
                return MqttAction.SUBSCRIBE;
            case ALL:
                return MqttAction.ALL;
            default:
                throw new IllegalArgumentException("Unknown MqttAction: " + entityAction);
        }
    }

    private static MqttPermissionEntity.MqttAction mapActionToEntity(MqttAction domainAction) {
        if (domainAction == null) {
            return null;
        }
        
        switch (domainAction) {
            case PUBLISH:
                return MqttPermissionEntity.MqttAction.PUBLISH;
            case SUBSCRIBE:
                return MqttPermissionEntity.MqttAction.SUBSCRIBE;
            case ALL:
                return MqttPermissionEntity.MqttAction.ALL;
            default:
                throw new IllegalArgumentException("Unknown MqttAction: " + domainAction);
        }
    }
}
