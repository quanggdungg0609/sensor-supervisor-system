package org.quangdung.infrastructure.component.mqtt.handler;

import java.time.LocalDateTime;
import java.util.Map;

import org.jboss.logging.Logger;
import org.quangdung.infrastructure.component.rabbitmq.RabbitMqMessageProducer;
import org.quangdung.infrastructure.component.rabbitmq.model.DevicePowerOutageModel;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PowerOutageHandler extends MessageHandler {
    private final Logger log;
    private final ObjectMapper objectMapper;
    private final RabbitMqMessageProducer rabbitMqMessageProducer;

    @Inject
    public PowerOutageHandler(
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
        return "power_outage";
    }

    /**
     * Handles power outage MQTT messages and forwards them to RabbitMQ
     * @param message the MQTT message containing power outage data
     * @return Uni<Void> representing the completion of message processing
     */
    @Override
    public Uni<Void> handle(MqttMessage<byte[]> message) {
        Map<String, Object> payloadMap;
        try {
            String data = new String(message.getPayload());
            payloadMap = objectMapper.readValue(data, new TypeReference<>() {});
        } catch (Exception e) {
            log.error("Failed to parse power outage payload for topic: " + message.getTopic(), e);
            return Uni.createFrom().failure(e);
        }

        String clientId = this.getClientId(message);
        Map<String, Object> dataMap = (Map<String, Object>) payloadMap.get("data");
        String timestampStr = (String) payloadMap.get("timestamp");

        if (dataMap == null || dataMap.isEmpty()) {
            log.warn("Empty or null data map for client: " + clientId);
            return Uni.createFrom().voidItem();
        }

        LocalDateTime timestamp = LocalDateTime.parse(timestampStr);

        DevicePowerOutageModel devicePowerOutageModel = DevicePowerOutageModel.builder()
            .clientId(clientId)
            .powerStatus((String) dataMap.get("power_status"))
            .timestamp(timestamp)
            .build();
            
        return rabbitMqMessageProducer.publishDevicePowerOutageUpdate(devicePowerOutageModel)
            .onItem().invoke(() -> log.infof("Power outage alert sent for client: %s", clientId))
            .onFailure().invoke(throwable -> log.errorf(throwable, "Failed to send power outage alert for client: %s", clientId));
    }
}
