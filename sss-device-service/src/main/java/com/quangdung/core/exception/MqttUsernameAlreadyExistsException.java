package com.quangdung.core.exception;

public class MqttUsernameAlreadyExistsException extends RuntimeException {
    public MqttUsernameAlreadyExistsException(String message){
        super(message);
    }
}
