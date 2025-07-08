package com.quangdung.infrastructure.entity.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateMqttAccountRequest {
    @JsonProperty("mqtt_username")
    private String mqttUsername;
    @JsonProperty("device_uuid")
    private String deviceUuid;
}
