package org.quangdung.infrastructure.component.rabbitmq.model;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@RegisterForReflection
public class DeviceStatusModel {
    private String clientId;
    private String status;
    private LocalDateTime timestamp;
}
