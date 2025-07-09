package org.quangdung.application.exception_handler;

import org.jboss.logging.Logger;
import org.quangdung.core.exception.MqttAccountNotExistsException;
import org.quangdung.core.exception.MqttUsernameAlreadyExistsException;
import org.quangdung.core.exception.ServiceCommunicationException;

import jakarta.inject.Inject;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import lombok.Data;

@Provider
public class MqttExceptionHandler implements ExceptionMapper<Exception>{
    private final Logger log;

    @Inject
    public MqttExceptionHandler(Logger log) {
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
            return Response.status(409)
                .entity(new ErrorResponse("CONFLICT", ex.getMessage()))
                .build();
        }else if(ex instanceof MqttAccountNotExistsException){
            return Response.status(404)
                .entity(new ErrorResponse("NOT_FOUND", ex.getMessage()))
                .build();
        } else if(ex instanceof ServiceCommunicationException){
            return Response.status(500)
                .entity(new ErrorResponse("SERVER_ERROR", "Internal server error"))
                .build();
        } else if (ex instanceof WebApplicationException) {
            throw (WebApplicationException) ex;
        }else{
            return Response.status(500)
                .entity(new ErrorResponse("SERVER_ERROR", "Internal server error"))
                .build();
        }
    }
   
}