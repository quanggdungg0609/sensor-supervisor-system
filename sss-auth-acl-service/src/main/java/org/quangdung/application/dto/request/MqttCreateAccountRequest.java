package org.quangdung.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MqttCreateAccountRequest {
    @JsonProperty("device_uuid")
    private String deviceUuid;
    @JsonProperty("mqtt_username")
    private String mqttUsername;
}
