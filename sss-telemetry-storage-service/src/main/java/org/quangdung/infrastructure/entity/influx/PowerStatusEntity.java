package org.quangdung.infrastructure.entity.influx;

import java.time.LocalDateTime;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

@Data
@RegisterForReflection
public class PowerStatusEntity {
    /**
     * The client ID of the device
     */
    private String clientId;
    
    /**
     * Timestamp when the power status event occurred
     */
    private LocalDateTime timestamp;
    
    /**
     * Power status of the device (1 = powered on, 0 = powered off)
     */
    private Integer powerStatus;
}