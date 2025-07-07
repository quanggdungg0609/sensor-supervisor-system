package org.quangdung.infrastructure.entity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;


import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
    name = "mqtt_permissions",
    indexes = {
        @Index(name = "idx_mqtt_permission_account_id", columnList = "mqtt_account_id"),
    }
)
public class MqttPermissionEntity extends PanacheEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mqtt_account_id", nullable = false)
    private MqttAccountEntity mqttAccount;

    @Column(name = "topic_pattern", nullable = false, length = 255)
    private String topicPattern;

    @Column(name = "mqtt_permission", nullable = false)
    @Enumerated(EnumType.STRING)
    private MqttPermission permission;

    @Column(name = "action", nullable = false)
    @Enumerated(EnumType.STRING)
    private MqttAction action;

    @ElementCollection(fetch = FetchType.EAGER)
    @Builder.Default
    private List<Integer> allowedQosLevels = Arrays.asList(0, 1, 2);

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.createdAt == null) {
            this.createdAt = now;
        }
        this.updatedAt = now;
        
        // Set default QoS levels if not already set
        if (this.allowedQosLevels == null || this.allowedQosLevels.isEmpty()) {
            this.allowedQosLevels = Arrays.asList(0, 1, 2);
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    public enum MqttAction {
        PUBLISH,
        SUBSCRIBE,
        ALL
    }

    public enum MqttPermission {
        ALLOW,
        DENY
    }
}
