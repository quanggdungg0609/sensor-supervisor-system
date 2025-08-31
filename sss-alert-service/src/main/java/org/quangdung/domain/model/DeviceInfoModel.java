package org.quangdung.domain.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeviceInfoModel {
    private String clientId;
    private String deviceUuid;
    private String mqttUsername;
    private String deviceName;
}
