package org.quangdung.infrastructure.entity.auth_acl_service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DeviceInfoResponse {
    private String deviceUuid;
    private String deviceName;
    private String mqttUsername;
    private String clientId;
}
