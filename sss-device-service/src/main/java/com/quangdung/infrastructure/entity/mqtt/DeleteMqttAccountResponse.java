package com.quangdung.infrastructure.entity.mqtt;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteMqttAccountResponse {
    @JsonProperty("message")
    private String message;
    
    @JsonProperty("device_uuid")
    private String deviceUuid;
}