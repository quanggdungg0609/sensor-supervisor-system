package org.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class MqttAccountInfoWithPass {
    @JsonProperty("mqtt_username")
    private String mqttUsername;
    @JsonProperty("mqtt_password")
    private String mqttPassword;
    @JsonProperty("client_id")
    private String clientId;
}
