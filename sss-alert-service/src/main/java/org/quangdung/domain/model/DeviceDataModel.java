package org.quangdung.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.quarkus.runtime.annotations.RegisterForReflection;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Data Transfer Object for device data messages from RabbitMQ
 * Contains device sensor data and metadata
 */
@RegisterForReflection
public class DeviceDataModel {
    @JsonProperty("clientId")
    private String clientId;
    
    @JsonProperty("timestamp")
    private LocalDateTime timestamp;
    
    @JsonProperty("data")
    private Map<String, Object> data;

    // Default constructor
    public DeviceDataModel() {}

    // Constructor with all fields
    public DeviceDataModel(String clientId, LocalDateTime timestamp, Map<String, Object> data) {
        this.clientId = clientId;
        this.timestamp = timestamp;
        this.data = data;
    }

    // Getters and Setters
    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }

    @Override
    public String toString() {
        return "DeviceDataModel{" +
                "clientId='" + clientId + '\'' +
                ", timestamp=" + timestamp +
                ", data=" + data +
                '}';
    }
}