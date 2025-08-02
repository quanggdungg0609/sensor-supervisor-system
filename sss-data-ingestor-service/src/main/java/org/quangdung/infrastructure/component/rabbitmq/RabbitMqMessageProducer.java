package org.quangdung.infrastructure.component.rabbitmq;

import org.jboss.logging.Logger;
import org.quangdung.core.metric.MetricService;
import org.quangdung.infrastructure.component.rabbitmq.model.DeviceDataModel;
import org.quangdung.infrastructure.component.rabbitmq.model.DevicePowerOutageModel;
import org.quangdung.infrastructure.component.rabbitmq.model.DeviceStatusModel;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.MutinyEmitter;
import io.smallrye.reactive.messaging.rabbitmq.OutgoingRabbitMQMetadata;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Message;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class RabbitMqMessageProducer {
    @Inject
    private Logger log;

    @Inject
    @Channel("device-status-updates")
    private MutinyEmitter<DeviceStatusModel> deviceStatusEmitter;

    @Inject
    @Channel("device-data-distribution")
    private MutinyEmitter<DeviceDataModel> deviceDataEmitter;

    @Inject
    @Channel("power-outage-alert")
    private MutinyEmitter<DevicePowerOutageModel> powerOutageEmitter;

    @Inject
    private MetricService metricsService;


    public Uni<Void> publishDeviceStatusUpdate(DeviceStatusModel deviceStatusModel) {
        log.infof("Publishing device status update: {}", deviceStatusModel.getClientId());

        // Record counter before message send
        metricsService.incrementCounter("rabbitmq_messages_published_total", 
                                        "channel", "device-status-updates",
                                        "status", "attempt");

        long startTime = System.nanoTime();


        Map<String, Object> headers = new HashMap<>();
        headers.put("clientId", deviceStatusModel.getClientId());
        headers.put("timestamp-sent", Instant.now().toString());
        OutgoingRabbitMQMetadata rabbitmqMetadata = OutgoingRabbitMQMetadata.builder()
                                                      .withHeaders(headers)
                                                      .build();
        Message<DeviceStatusModel> message = Message.of(deviceStatusModel).addMetadata(rabbitmqMetadata);

        return deviceStatusEmitter.sendMessage(message)
            .onItem().invoke(()->{
                long duration = System.nanoTime() - startTime; 
                metricsService.recordTimer("rabbitmq_publish_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                            "channel", "device-status-updates",
                                            "result", "success");
                metricsService.incrementCounter("rabbitmq_messages_published_total",
                                                "channel", "device-status-updates",
                                                "result", "success");
                log.infof("Device status update published successfully for client ID: %s", deviceStatusModel.getClientId());
            })
            .onFailure().invoke(throwable -> {
                long duration = System.nanoTime() - startTime; 
                metricsService.recordTimer("rabbitmq_publish_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                            "channel", "device-status-updates",
                                            "result", "failure");
                metricsService.incrementCounter("rabbitmq_messages_published_total",
                                                "channel", "device-status-updates",
                                                "result", "failure");
                log.errorf(throwable, "Failed to publish device status update for client ID: %s: %s", deviceStatusModel.getClientId(), throwable.getMessage());
            });
    }


    public Uni<Void> publishDeviceDataUpdate(DeviceDataModel deviceDataModel) {
        log.infof("Publishing device data update for client ID: %s", deviceDataModel.getClientId());


        metricsService.incrementCounter("rabbitmq_messages_published_total", 
                                        "channel", "device-data-distribution",
                                        "status", "attempt");

        long startTime = System.nanoTime();

        Map<String, Object> headers = new HashMap<>();
        headers.put("clientId", deviceDataModel.getClientId());
        headers.put("timestamp-sent", Instant.now().toString());
        OutgoingRabbitMQMetadata rabbitmqMetadata = OutgoingRabbitMQMetadata.builder()
                                                      .withHeaders(headers)
                                                      .build();
                                                      
        Message<DeviceDataModel> message = Message.of(deviceDataModel).addMetadata(rabbitmqMetadata);

        return deviceDataEmitter.sendMessage(message)
            .onItem().invoke(()->{
                long duration = System.nanoTime() - startTime;
                metricsService.recordTimer("rabbitmq_publish_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                            "channel", "device-data-distribution",
                                            "result", "success");
                metricsService.incrementCounter("rabbitmq_messages_published_total",
                                                "channel", "device-data-distribution",
                                                "result", "success");
                log.infof("Device data update published successfully for client ID: %s", deviceDataModel.getClientId());
            })
            .onFailure().invoke(throwable -> {
                long duration = System.nanoTime() - startTime;
                metricsService.recordTimer("rabbitmq_publish_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                            "channel", "device-data-distribution",
                                            "result", "failure");
                metricsService.incrementCounter("rabbitmq_messages_published_total",
                                                "channel", "device-data-distribution",
                                                "result", "failure");
                log.errorf(throwable, "Failed to publish device data update for client ID: %s: %s", deviceDataModel.getClientId(), throwable.getMessage());
            });
    }

    /**
     * Publishes device power outage alert to RabbitMQ
     * @param devicePowerOutageModel the power outage data model containing client ID, power status and timestamp
     * @return Uni<Void> representing the completion of the publish operation
     */
    public Uni<Void> publishDevicePowerOutageUpdate(DevicePowerOutageModel devicePowerOutageModel) {
        log.infof("Publishing device power outage update for client ID: %s", devicePowerOutageModel.getClientId());

        metricsService.incrementCounter("rabbitmq_messages_published_total", 
                                        "channel", "power-outage-alert",
                                        "status", "attempt");

        long startTime = System.nanoTime();
     
        Map<String, Object> headers = new HashMap<>();
        headers.put("clientId", devicePowerOutageModel.getClientId());
        headers.put("timestamp-sent", Instant.now().toString());
        OutgoingRabbitMQMetadata rabbitmqMetadata = OutgoingRabbitMQMetadata.builder()
                                                      .withHeaders(headers)
                                                      .build();
                                                      
        Message<DevicePowerOutageModel> message = Message.of(devicePowerOutageModel).addMetadata(rabbitmqMetadata);
        
        return powerOutageEmitter.sendMessage(message)
            .onItem().invoke(() -> {
                long duration = System.nanoTime() - startTime;
                metricsService.recordTimer("rabbitmq_publish_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                            "channel", "power-outage-alert",
                                            "result", "success");
                metricsService.incrementCounter("rabbitmq_messages_published_total",
                                                "channel", "power-outage-alert",
                                                "result", "success");
                log.infof("Device power outage alert published successfully for client ID: %s", devicePowerOutageModel.getClientId());
            })
            .onFailure().invoke(throwable -> {
                long duration = System.nanoTime() - startTime;
                metricsService.recordTimer("rabbitmq_publish_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                            "channel", "power-outage-alert",
                                            "result", "failure");
                metricsService.incrementCounter("rabbitmq_messages_published_total",
                                                "channel", "power-outage-alert",
                                                "result", "failure");
                log.errorf(throwable, "Failed to publish device power outage alert for client ID: %s: %s", devicePowerOutageModel.getClientId(), throwable.getMessage());
            });
    }
}
