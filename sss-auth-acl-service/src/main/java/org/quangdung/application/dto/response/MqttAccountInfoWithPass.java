package org.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttAccountInfoWithPass {
    @JsonProperty("mqtt_username")
    private String mqttUsername;
    @JsonProperty("mqtt_password")
    private String mqttPassword;
    @JsonProperty("client_id")
    private String clientId;
}
