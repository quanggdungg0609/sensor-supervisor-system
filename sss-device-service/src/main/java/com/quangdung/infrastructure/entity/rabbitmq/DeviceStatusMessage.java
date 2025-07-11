package com.quangdung.infrastructure.entity.rabbitmq;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class DeviceStatusMessage {
    private String status;
}
