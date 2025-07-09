package org.quangdung.infrastructure.component.mqtt.handler;

import java.time.LocalDateTime;
import java.util.Map;

import org.jboss.logging.Logger;
import org.quangdung.infrastructure.component.rabbitmq.RabbitMqMessageProducer;
import org.quangdung.infrastructure.component.rabbitmq.model.DeviceStatusModel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class StatusHandler extends MessageHandler {
    private final Logger log;
    private final ObjectMapper objectMapper;
    private final RabbitMqMessageProducer rabbitMqMessageProducer;

    @Inject
    public StatusHandler(
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
            log.error("Failed to parse status payload for topic: " + message.getTopic(), e);
            return Uni.createFrom().failure(e);
        }
        String clientId = this.getClientId(message);
        DeviceStatusModel deviceStatus = DeviceStatusModel.builder()
            .clientId(clientId)
            .status((String) payloadMap.get("status"))
            .timestamp(LocalDateTime.now())
            .build();

        return rabbitMqMessageProducer.publishDeviceStatusUpdate(deviceStatus)
            .onItem().invoke(() -> log.info("Device status update sent for client: " + clientId))
            .onFailure().invoke(throwable -> log.error(throwable));
    }
}
