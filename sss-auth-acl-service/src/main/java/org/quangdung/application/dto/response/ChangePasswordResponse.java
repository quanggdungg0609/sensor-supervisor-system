package org.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO for change password operation
 * Contains the new password (unhashed) and client information
 */
@Data
@Builder
@RegisterForReflection
public class ChangePasswordResponse {
    /**
     * The client ID that was updated
     */
    @JsonProperty("client_id")
    private String clientId;
    
    /**
     * The new password (unhashed) for the client
     */
    @JsonProperty("new_password")
    private String newPassword;
    
    /**
     * Success message
     */
    @JsonProperty("message")
    private String message;
}