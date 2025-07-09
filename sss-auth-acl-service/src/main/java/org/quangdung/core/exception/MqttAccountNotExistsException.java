package org.quangdung.core.exception;

public class MqttAccountNotExistsException extends RuntimeException {
    public MqttAccountNotExistsException(String message) {
        super(message);
    }
}
