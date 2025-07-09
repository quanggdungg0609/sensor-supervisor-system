package org.quangdung.domain.entity;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class DeviceInfo {
    private String deviceUuid;
    private String deviceName;
    private String mqttUsername;
    private String clientId;
}
