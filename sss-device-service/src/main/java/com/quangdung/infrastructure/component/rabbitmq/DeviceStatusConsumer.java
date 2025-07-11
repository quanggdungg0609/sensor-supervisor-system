package com.quangdung.infrastructure.component.rabbitmq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quangdung.core.exception.DeviceNotFoundException;
import com.quangdung.infrastructure.dao.mqtt_dao.MqttDAO;
import com.quangdung.infrastructure.entity.device_entity.DeviceEntity;
import com.quangdung.infrastructure.entity.rabbitmq.DeviceStatusMessage;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.annotations.Merge;
import io.smallrye.reactive.messaging.rabbitmq.IncomingRabbitMQMetadata;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Optional;

@ApplicationScoped
public class DeviceStatusConsumer {
    @Inject
    Logger log;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    @RestClient
    MqttDAO mqttDAO;

    @Merge
    @WithTransaction
    @Incoming("device-status-updates")
    public Uni<Void> processDeviceStatusUpdate(Message<JsonObject> message) {
        final DeviceStatusMessage deviceStatusMessage;
        try {
            deviceStatusMessage = objectMapper.readValue(message.getPayload().encode(), DeviceStatusMessage.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize message payload. Acknowledging and discarding.", e);
            return Uni.createFrom().completionStage(message.ack());
        }
        log.infof("Received device status update: %s", deviceStatusMessage);

        Optional<String> clientIdOptional = message.getMetadata(IncomingRabbitMQMetadata.class)
                .map(metadata -> metadata.getHeaders().get("clientId"))
                .map(Object::toString);

        if (clientIdOptional.isEmpty()) {
            log.warn("Cannot find 'clientId' in message headers. Acknowledging and discarding.");
            return Uni.createFrom().completionStage(message.ack());
        }
        final String clientId = clientIdOptional.get();
        log.infof("Processing status update for clientId: %s", clientId);

        return mqttDAO.getMqttUsernameByClientId(clientId).onItem().transformToUni(
            response ->{
                log.info("Found mqttUsername: " + response.getMqttUsername() );
                return DeviceEntity.<DeviceEntity>find("mqttUsername", response.getMqttUsername()).firstResult()
                    .onItem().transform(entity ->{
                        if (entity == null){
                            log.warn("Device not found");
                            message.nack(new DeviceNotFoundException("Device not found"));
                            return Uni.createFrom().nullItem();
                        }
                        DeviceEntity.DeviceStatus status = switch(deviceStatusMessage.getStatus()){
                            case "active" -> DeviceEntity.DeviceStatus.ACTIVE;
                            case "inactive" -> DeviceEntity.DeviceStatus.INACTIVE;
                            default -> DeviceEntity.DeviceStatus.INACTIVE;
                        };
                        entity.setStatus(status);
                        return entity.persist().onItem().invoke(()-> message.ack());
                    })
                    .onFailure().invoke((throwable)->{
                        log.warn(throwable.getMessage());
                        message.nack(throwable);
                    });
            }
        )
        .onFailure().invoke(throwable -> {
            log.warn(throwable.getMessage());
            message.ack();
        }).replaceWithVoid();
    }
}