package org.quangdung.infrastructure.component.mqtt.handler;

import io.smallrye.mutiny.Uni;
import io.smallrye.reactive.messaging.mqtt.MqttMessage;

public abstract class MessageHandler {
    public abstract String getHandledTopicType();
    public abstract Uni<Void> handle(MqttMessage<byte[]> message); 

    protected String getClientId(MqttMessage<byte[]> message){
        String topic = message.getTopic();
        String[] topicParts = topic.split("/");
        if (topicParts.length < 2) {
            return "";
        }
        return topicParts[1];
    }
}
