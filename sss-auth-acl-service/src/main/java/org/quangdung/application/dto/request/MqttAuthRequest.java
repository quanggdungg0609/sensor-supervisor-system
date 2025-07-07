package org.quangdung.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MqttAuthRequest {
     @JsonProperty("clientid")
    private String clientId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("password")
    private String password;
    
    @JsonProperty("peerhost")
    private String peerHost;
    
    @JsonProperty("mountpoint")
    private String mountpoint;
}
