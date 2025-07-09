package org.quangdung.infrastructure.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;


import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Entity
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(
    name = "mqtt_accounts",
    indexes = {
        @Index( name= "idx_mqtt_account_device_uuid", columnList = "device_uuid"),
        @Index(name = "idx_mqtt_account_mqtt_username", columnList = "mqtt_username"),
        @Index(name = "idx_mqtt_account_client_id", columnList = "client_id"),
    }
)
public class MqttAccountEntity extends PanacheEntity{
    @Column(name="mqtt_username", nullable = false)
    private String mqttUsername;

    @Column(name="client_id", nullable = false)
    private String clientId;

    @Column(name="mqtt_password", nullable = false)
    private String mqttPassword;

    @Column(name="device_uuid")
    private String deviceUuid;

    @Builder.Default
    @OneToMany(
        mappedBy = "mqttAccount", 
        cascade = CascadeType.ALL, 
        fetch = FetchType.LAZY,
        orphanRemoval = true
    )
    private List<MqttPermissionEntity> permissions = new ArrayList<>();

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void addPermission(MqttPermissionEntity permission) {
        if (permissions == null) {
            permissions = new ArrayList<>();
        }
        permissions.add(permission);
        permission.setMqttAccount(this);
    }
    
    /**
     * Removes a permission from this MQTT account
     * @param permission The permission to remove
     */
    public void removePermission(MqttPermissionEntity permission) {
        permissions.remove(permission);
        permission.setMqttAccount(null);
    }
}
