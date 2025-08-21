package org.quangdung.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for device status messages from RabbitMQ
 * Contains device operational status information
 */
@RegisterForReflection
public class DeviceStatusModel {
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("status")
    private String status;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    // Default constructor
    public DeviceStatusModel() {}

    // Constructor with all fields
    public DeviceStatusModel(String clientId, String status, LocalDateTime timestamp) {
        this.clientId = clientId;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DeviceStatusModel{" +
                "clientId='" + clientId + '\'' +
                ", status='" + status + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}