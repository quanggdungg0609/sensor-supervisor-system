package org.quangdung.infrastructure.component.mqtt;

import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.quangdung.core.metric.MetricService;
import org.quangdung.infrastructure.component.mqtt.dispatcher.MqttMessageDispatcher;
import org.quangdung.infrastructure.component.mqtt.handler.MessageHandler;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttClientComponent {
    private final Logger log;
    private final MqttMessageDispatcher dispatcher;
    private final MetricService metricsService;
    
    @Inject
    public MqttClientComponent(
        Logger log,
        MqttMessageDispatcher dispatcher,
        MetricService metricsService
    ) {
        this.log = log;
        this.dispatcher = dispatcher;
        this.metricsService = metricsService;
    }

    @Incoming("device-data-in")
    public Uni<Void> process(MqttMessage<byte[]> message) {
        String topic = message.getTopic();
        String topicType = getLastSegmentFromTopic(topic);
        String channel = "device-data-in"; // Name of your incoming MQTT channel

        // Record the total number of messages received
        metricsService.incrementCounter("mqtt_messages_received_total", 
                                        "channel", channel, 
                                        "topic", topic);

        log.infof("Processing MQTT message for topic: %s, type: %s", topic, topicType);
        MessageHandler handler = dispatcher.getHandler(topicType);

        if (handler != null) {
            long startTime = System.nanoTime(); // Start timing the processing

            return handler.handle(message)
                .onItem().invoke(() -> {
                    long duration = System.nanoTime() - startTime;
                    // Record processing duration and total successfully processed messages
                    metricsService.recordTimer("mqtt_message_processing_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                                "channel", channel,
                                                "topic_type", topicType,
                                                "result", "success");
                    metricsService.incrementCounter("mqtt_messages_processed_total",
                                                    "channel", channel,
                                                    "topic_type", topicType,
                                                    "result", "success");
                    log.infof("Successfully processed message for topic: %s", topic);
                })
                .onFailure().invoke(throwable -> {
                    long duration = System.nanoTime() - startTime;
                    // Record processing duration and total failed messages
                    metricsService.recordTimer("mqtt_message_processing_duration_seconds", duration, TimeUnit.NANOSECONDS,
                                                "channel", channel,
                                                "topic_type", topicType,
                                                "result", "failure");
                    metricsService.incrementCounter("mqtt_messages_processed_total",
                                                    "channel", channel,
                                                    "topic_type", topicType,
                                                    "result", "failure");
                    log.error("Failed to process message for topic: " + topic, throwable);
                })
                .onFailure().recoverWithItem(() -> {
                    // Recover from failures to prevent message processing from stopping
                    log.warn("Recovered from failure for topic: " + topic);
                    return null; 
                });
        } else {
            // Record the number of messages for which no handler was found
            metricsService.incrementCounter("mqtt_no_handler_found_total", 
                                            "channel", channel, 
                                            "topic_type", topicType);
            log.warn("No handler found for topic type: " + topicType);
            return Uni.createFrom().voidItem();
        }
    }

    private String getLastSegmentFromTopic(String topic) {
        String[] parts = topic.split("/");
        return parts[parts.length - 1];
    }
}
