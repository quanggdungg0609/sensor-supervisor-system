package com.quangdung.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class CreateDeviceRequest {
    @JsonProperty("device_name")
    private String deviceName;

    @JsonProperty("mqtt_username")
    private String mqttUsername;
}
