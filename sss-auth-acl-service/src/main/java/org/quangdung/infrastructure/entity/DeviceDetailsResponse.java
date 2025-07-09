package org.quangdung.infrastructure.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@RegisterForReflection 
public class DeviceDetailsResponse {
    @JsonProperty("device_uuid")
    private String deviceUuid;

    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("mqtt_username")
    private String mqttUsername; 
}