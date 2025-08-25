package com.quangdung.core.exception;

public class UpdateDeviceException extends RuntimeException {
    public UpdateDeviceException(String message) {
        super(message);
    }
    
    public UpdateDeviceException(String message, Throwable cause) {
        super(message, cause);
    }
}