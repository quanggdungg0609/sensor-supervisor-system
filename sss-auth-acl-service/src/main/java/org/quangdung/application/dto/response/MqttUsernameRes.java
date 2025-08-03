package org.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class MqttUsernameRes {
    @JsonProperty("mqtt_username")
    private String mqttUsername; 
}
