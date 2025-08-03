package org.quangdung.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class MqttCreateAccountRequest {
    @JsonProperty("device_uuid")
    private String deviceUuid;
    @JsonProperty("mqtt_username")
    private String mqttUsername;
}
