package org.quangdung.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for power outage alert messages from RabbitMQ
 * Contains power status information for devices
 */
@RegisterForReflection
public class DevicePowerOutageModel {
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("powerStatus")
    private Integer powerStatus;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;

    // Default constructor
    public DevicePowerOutageModel() {}

    // Constructor with all fields
    public DevicePowerOutageModel(String clientId, Integer powerStatus, LocalDateTime timestamp) {
        this.clientId = clientId;
        this.powerStatus = powerStatus;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public Integer getPowerStatus() {
        return powerStatus;
    }

    public void setPowerStatus(Integer powerStatus) {
        this.powerStatus = powerStatus;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "DevicePowerOutageModel{" +
                "clientId='" + clientId + '\'' +
                ", powerStatus=" + powerStatus +
                ", timestamp=" + timestamp +
                '}';
    }
}