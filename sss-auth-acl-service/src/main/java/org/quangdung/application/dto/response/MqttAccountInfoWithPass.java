package org.quangdung.application.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MqttAccountInfoWithPass {
    private String mqttUsername;
    private String mqttPassword;
    private String clientId;
}
