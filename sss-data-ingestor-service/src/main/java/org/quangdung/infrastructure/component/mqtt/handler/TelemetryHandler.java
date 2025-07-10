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
        Map<String, Object> payloadMap;
        try {
            String data = new String(message.getPayload());
            payloadMap = objectMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse telemetry payload for topic: " + message.getTopic(), e);
            return Uni.createFrom().failure(e);
        }
        String clientId = this.getClientId(message);
        Map<String, Object> dataMap = (Map<String, Object>) payloadMap.get("data");
        String timestampStr = (String) payloadMap.get("timestamp");

        if (dataMap == null || dataMap.isEmpty()) {
            log.warn("Empty or null data map for client: " + clientId);
            return Uni.createFrom().voidItem();
        }

        DeviceDataModel data = DeviceDataModel.builder()
            .clientId(clientId)
            .data(dataMap)
            .timestamp(LocalDateTime.ofInstant(Instant.parse(timestampStr), ZoneOffset.UTC))
            .build();
        return rabbitMqMessageProducer.publishDeviceDataUpdate(data)
            .onItem().invoke(() -> log.info("Device data update sent for client: " + clientId))
            .onFailure().invoke(throwable -> log.error(throwable));
    }
}
