package com.quangdung.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceInfo {
    private String deviceUuid;
    private String deviceName;
    private String mqttUsername;
}
