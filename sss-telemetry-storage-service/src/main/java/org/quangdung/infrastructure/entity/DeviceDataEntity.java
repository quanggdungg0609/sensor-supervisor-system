package org.quangdung.infrastructure.entity;

import java.time.LocalDateTime;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class DeviceDataEntity {
    private String clientId;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
}
