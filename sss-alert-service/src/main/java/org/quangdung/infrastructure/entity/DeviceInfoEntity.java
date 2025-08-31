package org.quangdung.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class DeviceInfoEntity {
    @JsonProperty("client_id")
    private String clientId;
    
    @JsonProperty("device_uuid")
    private String deviceUuid;
    
    @JsonProperty("mqtt_username")
    private String mqttUsername;
    
    @JsonProperty("device_name")
    private String deviceName;
}
