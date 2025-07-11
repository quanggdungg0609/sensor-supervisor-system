package com.quangdung.infrastructure.entity.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class GetMqttUsernameResponse {
    @JsonProperty("mqtt_username")
    private String mqttUsername;
}
