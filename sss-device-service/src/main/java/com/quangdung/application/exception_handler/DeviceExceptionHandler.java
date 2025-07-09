package com.quangdung.application.exception_handler;

import org.jboss.logging.Logger;

import com.quangdung.core.exception.DeviceNotFoundException;
import com.quangdung.core.exception.InvalidMqttUsernameException;
import com.quangdung.core.exception.MqttUsernameAlreadyExistsException;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Data;

@Provider
public class DeviceExceptionHandler implements ExceptionMapper<Exception>{
    private final Logger log;

    @Inject
    public DeviceExceptionHandler(Logger log) {
        this.log = log;
    }

    @Data
    private class ErrorResponse{
        private String errorCode;
        private String message;

        public ErrorResponse(String errorCode, String message){
            this.errorCode = errorCode;
            this.message = message;
        }
    }

    @Override
    public Response toResponse(Exception ex) {
        log.error(ex);
        if(ex instanceof MqttUsernameAlreadyExistsException) {
            return Response.status(Response.Status.CONFLICT)
                        .entity(
                            new ErrorResponse("CONFLICT", ex.getMessage())
                        ).build();
        }
        else if(ex instanceof InvalidMqttUsernameException){
            return Response.status(Response.Status.BAD_REQUEST)
                        .entity(
                            new ErrorResponse("BAD_REQUEST", ex.getMessage())
                        ).build();
        }else if(ex instanceof DeviceNotFoundException){
            return Response.status(Response.Status.NOT_FOUND)
                        .entity(
                            new ErrorResponse("NOT_FOUND", ex.getMessage())
                        ).build();
        }else if(ex instanceof NotFoundException){
            return Response.status(Response.Status.NOT_FOUND)
                        .entity(new ErrorResponse("NOT_FOUND", ex.getMessage()))
                        .build();
        }else{
            return Response.status(500)
                .entity(new ErrorResponse("SERVER_ERROR", "Internal server error"))
                .build();
        }
    }
}
