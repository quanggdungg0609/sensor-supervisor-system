package org.quangdung.infrastructure.component.mqtt.dispatcher;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.quangdung.infrastructure.component.mqtt.handler.MessageHandler;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttMessageDispatcher {
    
    @Inject
    Instance<MessageHandler> handlers;

    private Map<String, MessageHandler> handlerMap;

    @PostConstruct
    void init() {
        this.handlerMap = handlers.stream()
                .collect(Collectors.toMap(MessageHandler::getHandledTopicType, Function.identity()));
    }

    public MessageHandler getHandler(String topicType) {
        return handlerMap.get(topicType);
    }
}
