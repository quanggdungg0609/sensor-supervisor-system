package org.quangdung.application.rabbitmq_listerner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.quangdung.domain.model.DeviceStatusModel;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * RabbitMQ listener for device status exchange messages
 * Monitors device health and connectivity status
 */
@ApplicationScoped
public class DeviceStatusListener {
    
    private static final Logger LOG = Logger.getLogger(DeviceStatusListener.class);
    
    @Inject
    ISendMailAlertUseCase sendMailAlertUseCase;
    
    private final ObjectMapper objectMapper;
    
    public DeviceStatusListener() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
    }

    /**
     * Listens to device-status-updates channel and processes device status updates
     * @param message Raw JSON message from RabbitMQ queue
     */
    @Incoming("device-status-updates")
    @Blocking
    public void processDeviceStatus(String message) {
        try {
            LOG.infof("Received device status message: %s", message);
            
            DeviceStatusModel deviceStatus = objectMapper.readValue(message, DeviceStatusModel.class);
            
            // Process device status
            LOG.infof("Processing status for device: %s, Status: %s at %s", 
                     deviceStatus.getClientId(), deviceStatus.getStatus(), deviceStatus.getTimestamp());
            
            // Check for critical device conditions
            if (isCriticalStatus(deviceStatus)) {
                String alertSubject = String.format("Device Critical Status - %s", deviceStatus.getClientId());
                String alertBody = String.format(
                    "Device %s requires attention:\n" +
                    "Status: %s\n" +
                    "Timestamp: %s",
                    deviceStatus.getClientId(),
                    deviceStatus.getStatus(),
                    deviceStatus.getTimestamp()
                );
                
                // sendMailAlertUseCase.sendAlert(alertSubject, alertBody);
            }
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing device status message: %s", message);
        }
    }
    
    /**
     * Determines if device status requires immediate attention
     * @param deviceStatus The device status to evaluate
     * @return true if status is critical, false otherwise
     */
    private boolean isCriticalStatus(DeviceStatusModel deviceStatus) {
        return "OFFLINE".equalsIgnoreCase(deviceStatus.getStatus()) ||
               "ERROR".equalsIgnoreCase(deviceStatus.getStatus()) ||
               "DISCONNECTED".equalsIgnoreCase(deviceStatus.getStatus());
    }
}