package com.quangdung.domain.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.quangdung.infrastructure.entity.device_entity.DeviceEntity;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Device {
    private UUID deviceUuid;
    private String deviceName;
    private String mqttUsername;
    private String clientId;
    private DeviceStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public enum DeviceStatus{
        ACTIVE,
        INACTIVE
    }

    /**
     * Convert domain entity to database entity
     * @return DeviceEntity for database persistence
     */
    public DeviceEntity toEntity(){
        DeviceEntity.DeviceStatus entityStatus;
        if(this.status != null){
           entityStatus = DeviceEntity.DeviceStatus.valueOf(this.status.name());
        }else{
            entityStatus = DeviceEntity.DeviceStatus.INACTIVE;
        }
        DeviceEntity entity = DeviceEntity.builder()
            .deviceName(this.deviceName)
            .mqttUsername(this.mqttUsername)
            .clientId(this.clientId)
            .createdAt(this.createdAt)
            .updatedAt(this.updatedAt)
            .status(entityStatus)
            .build();

        return entity;
    }

    /**
     * Create domain entity from database entity
     * @param entity Database entity
     * @return Domain entity
     */
    public static Device fromEntity(DeviceEntity entity){
        if (entity == null) {
            return null;
        }
        
        DeviceStatus domainStatus = null;
        if (entity.getStatus() != null) {
            domainStatus = DeviceStatus.valueOf(entity.getStatus().name());
        }
        
        return Device.builder()
            .deviceUuid(entity.getDeviceUuid())
            .deviceName(entity.getDeviceName())
            .mqttUsername(entity.getMqttUsername())
            .clientId(entity.getClientId())
            .createdAt(entity.getCreatedAt())
            .updatedAt(entity.getUpdatedAt())
            .status(domainStatus)
            .build();
    }

}
