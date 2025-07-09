package com.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceInfo {
    @JsonProperty("device_uuid")
    private String deviceUuid;
    @JsonProperty("device_name")
    private String deviceName;
    @JsonProperty("mqtt_username")
    private String mqttUsername;
}
