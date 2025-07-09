package org.quangdung.infrastructure.component.rabbitmq.model;

import java.time.LocalDateTime;
import java.util.Map;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class DeviceDataModel {
    private String clientId;
    private LocalDateTime timestamp;
    private Map<String, Object> data;
}
