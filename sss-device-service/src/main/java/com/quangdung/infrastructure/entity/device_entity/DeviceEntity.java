package com.quangdung.infrastructure.entity.device_entity;

import java.time.LocalDateTime;
import java.util.UUID;


import io.quarkus.hibernate.reactive.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "devices", indexes = {
    @Index(name = "idx_device_device_name", columnList = "device_name"),
    @Index(name = "idx_device_mqtt_username", columnList = "mqtt_username")
})
public class DeviceEntity extends PanacheEntityBase{
    @Id
    @GeneratedValue(generator = "UUID")
    // @GenericGenerator(
    //     name = "UUID",
    //     strategy = "org.hibernate.id.UUIDGenerator"
    // )
    @Column(name = "device_uuid", nullable = false, unique = true, updatable = false)
    private UUID deviceUuid;

    @Column(name = "device_name")
    private String deviceName;


    @Column(name = "mqtt_username", nullable = false, unique = true)
    private String mqttUsername;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private DeviceStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = DeviceStatus.INACTIVE;
        }
    }

    @PostUpdate
    public void postUpdate(){
        this.updatedAt = LocalDateTime.now();
    }

    public enum DeviceStatus{
        ACTIVE,
        INACTIVE
    }

    
}
