package com.quangdung.infrastructure.entity.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CreateMqttAccountResponse {
    @JsonProperty("mqtt_username")
    private String mqttUsername;
    @JsonProperty("mqtt_password")
    private String mqttPassword;
    @JsonProperty("client_id")
    private String clientId;
}
