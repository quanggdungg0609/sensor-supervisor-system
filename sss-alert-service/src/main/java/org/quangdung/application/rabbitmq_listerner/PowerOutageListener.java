package org.quangdung.application.rabbitmq_listerner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.quangdung.domain.model.DevicePowerOutageModel;
import org.quangdung.domain.usecase.interfaces.IGetDeviceByClientIdUseCase;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import io.smallrye.common.annotation.Blocking;
import io.smallrye.mutiny.Uni;

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

    @Inject
    IGetDeviceByClientIdUseCase getDeviceByClientIdUseCase;
    
    private final ObjectMapper objectMapper;

    public PowerOutageListener() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Listens to power-outage-alert channel and processes power outage alerts
     * @param jsonMessage Message containing JsonObject from RabbitMQ queue
     * @return Uni<Void> representing the completion of the processing
     */
    @Incoming("power-outage-alert")
    @Blocking
    public Uni<Void> processPowerOutageAlert(Message<JsonObject> jsonMessage) {
        try {
            JsonObject jsonPayload = jsonMessage.getPayload();
            String jsonString = jsonPayload.toString();
            LOG.infof("Received power outage alert: %s", jsonString);
            
            DevicePowerOutageModel powerOutage = objectMapper.readValue(jsonString, DevicePowerOutageModel.class);
            
            // Process power outage alert
            LOG.infof("Processing power outage alert for client: %s, Power Status: %d at %s", 
                     powerOutage.getClientId(), powerOutage.getPowerStatus(), powerOutage.getTimestamp());
            
            // Send email alert for power outage (assuming 0 = power off, 1 = power on)
            if (powerOutage.getPowerStatus() != null && powerOutage.getPowerStatus() == 0) {
                
                return getDeviceByClientIdUseCase.execute(powerOutage.getClientId())
                    .onItem().transformToUni(device -> {
                        String alertSubject = String.format("[CRITIQUE] Alerte de Panne d'Alimentation - Appareil [%s] - ClientID: %s", 
                            device.getDeviceName(), device.getClientId());
                        
                        String alertBody = String.format(
                            "Panne d'alimentation détectée pour l'appareil:\n\n" +
                            "Nom de l'appareil: %s\n" +
                            "ID Client: %s\n" +
                            "UUID de l'appareil: %s\n" +
                            "Nom d'utilisateur MQTT: %s\n" +
                            "État de l'alimentation: %s\n" +
                            "Horodatage: %s\n\n" +
                            "Veuillez vérifier l'appareil immédiatement car il pourrait subir une panne d'alimentation.",
                            device.getDeviceName(),
                            device.getClientId(),
                            device.getDeviceUuid(),
                            device.getMqttUsername(),
                            powerOutage.getPowerStatus() == 0 ? "ALIMENTATION COUPÉE (PANNE)" : "ALIMENTATION ACTIVÉE",
                            powerOutage.getTimestamp()
                        );
                        
                        LOG.infof("Sending power outage email alert for device: %s (ClientID: %s)", 
                            device.getDeviceName(), device.getClientId());
                        
                        return sendMailAlertUseCase.execute(alertSubject, alertBody);
                    })
                    .onFailure().recoverWithUni(throwable -> {
                        LOG.errorf(throwable, "Failed to get device info for clientId: %s, sending basic alert", 
                            powerOutage.getClientId());
                        
                        // Fallback: send basic alert without device details
                        String fallbackSubject = String.format("[CRITIQUE] Alerte de Panne d'Alimentation - ClientID: %s", 
                            powerOutage.getClientId());
                        
                        String fallbackBody = String.format(
                            "Panne d'alimentation détectée pour l'appareil:\n\n" +
                            "ID Client: %s\n" +
                            "État de l'alimentation: %s\n" +
                            "Horodatage: %s\n\n" +
                            "Note: Impossible de récupérer les informations détaillées de l'appareil.",
                            powerOutage.getClientId(),
                            powerOutage.getPowerStatus() == 0 ? "ALIMENTATION COUPÉE (PANNE)" : "ALIMENTATION ACTIVÉE",
                            powerOutage.getTimestamp()
                        );
                        
                        return sendMailAlertUseCase.execute(fallbackSubject, fallbackBody);
                    })
                    .onItem().invoke(() -> {
                        LOG.infof("Successfully processed power outage alert for clientId: %s", 
                            powerOutage.getClientId());
                        jsonMessage.ack();
                    })
                    .onFailure().invoke(throwable -> {
                        LOG.errorf(throwable, "Failed to send power outage email for clientId: %s", 
                            powerOutage.getClientId());
                        jsonMessage.ack(); // Still ack to avoid reprocessing
                    })
                    .replaceWithVoid();
            } else {
                LOG.infof("Power status is normal (%d) for clientId: %s, no alert needed", 
                    powerOutage.getPowerStatus(), powerOutage.getClientId());
                jsonMessage.ack();
                return Uni.createFrom().voidItem();
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing power outage alert: %s", jsonMessage.getPayload().toString());
            jsonMessage.ack(); // Ack to avoid infinite reprocessing
            return Uni.createFrom().voidItem();
        }
    }
}