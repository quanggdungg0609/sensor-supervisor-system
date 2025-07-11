package org.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttUsernameRes {
    @JsonProperty("mqtt_username")
    private String mqttUsername; 
}
