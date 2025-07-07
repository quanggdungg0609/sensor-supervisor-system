package org.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttResponse {

    @JsonProperty("result")
    private String result; // "allow", "deny", or "ignore"
    
    /**
     * Creates an allow response
     */
    public static MqttResponse allow() {
        return MqttResponse.builder()
            .result("allow")
            .build();
    }
    
    /**
     * Creates a deny response
     */
    public static MqttResponse deny() {
        return MqttResponse.builder()
            .result("deny")
            .build();
    }
    
    /**
     * Creates an ignore response
     */
    public static MqttResponse ignore() {
        return MqttResponse.builder()
            .result("ignore")
            .build();
    }
}

