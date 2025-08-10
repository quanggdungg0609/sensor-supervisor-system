package org.quangdung.infrastructure.component.mqtt.handler;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Map;

import org.jboss.logging.Logger;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.quangdung.infrastructure.component.rabbitmq.RabbitMqMessageProducer;
import org.quangdung.infrastructure.component.rabbitmq.model.DeviceDataModel;

@ApplicationScoped
public class TelemetryHandler extends MessageHandler {
    private final Logger log;
    private final ObjectMapper objectMapper;
    private final RabbitMqMessageProducer rabbitMqMessageProducer;

    @Inject
    public TelemetryHandler(
        Logger log,
        ObjectMapper objectMapper,
        RabbitMqMessageProducer rabbitMqMessageProducer
    ) {
        this.log = log;
        this.objectMapper = objectMapper;
        this.rabbitMqMessageProducer = rabbitMqMessageProducer;
    }

    @Override
    public String getHandledTopicType() {
        return "telemetry";
    }

    @Override
    public Uni<Void> handle(MqttMessage<byte[]> message) {
        String data = new String(message.getPayload());
        String clientId = this.getClientId(message);
        
        // Log all received messages
        log.infof("Received telemetry message from client: %s, topic: %s, payload: %s", 
                 clientId, message.getTopic(), data);
        
        Map<String, Object> payloadMap;
        try {
            payloadMap = objectMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse telemetry payload for topic: " + message.getTopic(), e);
            return Uni.createFrom().failure(e);
        }
        
        Map<String, Object> dataMap = (Map<String, Object>) payloadMap.get("data");
        
        // Fix: Handle both String and Integer timestamp formats
        Object timestampObj = payloadMap.get("timestamp");
        LocalDateTime timestamp;
        
        try {
            if (timestampObj instanceof String) {
                // Handle ISO string format: "2024-01-15T10:30:00Z"
                timestamp = LocalDateTime.ofInstant(Instant.parse((String) timestampObj), ZoneOffset.UTC);
            } else if (timestampObj instanceof Integer) {
                // Handle Unix timestamp (seconds): 1723234077
                timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(((Integer) timestampObj).longValue()), ZoneOffset.UTC);
            } else if (timestampObj instanceof Long) {
                // Handle Unix timestamp (milliseconds): 1723234077000
                long timestampValue = (Long) timestampObj;
                if (timestampValue > 1_000_000_000_000L) {
                    // Milliseconds
                    timestamp = LocalDateTime.ofInstant(Instant.ofEpochMilli(timestampValue), ZoneOffset.UTC);
                } else {
                    // Seconds
                    timestamp = LocalDateTime.ofInstant(Instant.ofEpochSecond(timestampValue), ZoneOffset.UTC);
                }
            } else {
                log.warn("Invalid timestamp format for client: " + clientId + ", using current time");
                timestamp = LocalDateTime.now(ZoneOffset.UTC);
            }
        } catch (Exception e) {
            log.error("Failed to parse timestamp for client: " + clientId + ", using current time", e);
            timestamp = LocalDateTime.now(ZoneOffset.UTC);
        }
    
        if (dataMap == null || dataMap.isEmpty()) {
            log.warn("Empty or null data map for client: " + clientId);
            return Uni.createFrom().voidItem();
        }
        
        log.infof("Processing telemetry for client: %s, data size: %d, timestamp: %s", 
                 clientId, dataMap.size(), timestamp);
    
        DeviceDataModel deviceData = DeviceDataModel.builder()
            .clientId(clientId)
            .data(dataMap)
            .timestamp(timestamp)
            .build();
            
        return rabbitMqMessageProducer.publishDeviceDataUpdate(deviceData)
            .onItem().invoke(() -> log.infof("Device data update sent for client: %s", clientId))
            .onFailure().invoke(throwable -> log.errorf(throwable, "Failed to send device data update for client: %s", clientId));
    }
}
