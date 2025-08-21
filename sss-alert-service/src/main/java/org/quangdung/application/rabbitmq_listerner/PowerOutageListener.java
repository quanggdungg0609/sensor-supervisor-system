package org.quangdung.application.rabbitmq_listerner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.quangdung.domain.model.DevicePowerOutageModel;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.vertx.core.json.JsonObject;

/**
 * RabbitMQ listener for power outage alert exchange messages
 * Processes power outage alerts and sends notifications
 */
@ApplicationScoped
public class PowerOutageListener {
    
    private static final Logger LOG = Logger.getLogger(PowerOutageListener.class);
    
    @Inject
    ISendMailAlertUseCase sendMailAlertUseCase;
    
    private final ObjectMapper objectMapper;

    public PowerOutageListener() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Listens to power-outage-alert channel and processes power outage alerts
     * @param jsonMessage JsonObject message from RabbitMQ queue
     */
    @Incoming("power-outage-alert")
    @Blocking
    public void processPowerOutageAlert(JsonObject jsonMessage) {
        try {
            String message = jsonMessage.toString();
            LOG.infof("Received power outage alert: %s", message);
            
            DevicePowerOutageModel powerOutage = objectMapper.readValue(message, DevicePowerOutageModel.class);
            
            // Process power outage alert
            LOG.infof("Processing power outage alert for client: %s, Power Status: %d at %s", 
                     powerOutage.getClientId(), powerOutage.getPowerStatus(), powerOutage.getTimestamp());
            
            // Send email alert for power outage (assuming 0 = power off, 1 = power on)
            if (powerOutage.getPowerStatus() != null && powerOutage.getPowerStatus() == 0) {
                String alertSubject = String.format("Power Outage Alert - Device %s", powerOutage.getClientId());
                String alertBody = String.format(
                    "Power outage detected for device: %s\n" +
                    "Power Status: %s\n" +
                    "Timestamp: %s",
                    powerOutage.getClientId(),
                    powerOutage.getPowerStatus() == 0 ? "OFF" : "ON",
                    powerOutage.getTimestamp()
                );
                
                // sendMailAlertUseCase.sendAlert(alertSubject, alertBody);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing power outage alert: %s", jsonMessage.toString());
        }
    }
}