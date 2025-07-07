package org.quangdung.application.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class MqttAclRequest {
    @JsonProperty("clientid")
    private String clientId;
    
    @JsonProperty("username")
    private String username;
    
    @JsonProperty("topic")
    private String topic;
    
    @JsonProperty("action")
    private String action; // "publish" or "subscribe"
    
    @JsonProperty("qos")
    private Integer qos; // QoS level: 0, 1, or 2
    
    @JsonProperty("peerhost")
    private String peerHost;
    
    @JsonProperty("protocol")
    private String protocol;
    
    @JsonProperty("mountpoint")
    private String mountpoint;
}
