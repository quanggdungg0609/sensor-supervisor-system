package org.quangdung.application.rabbitmq_listerner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.quangdung.domain.model.DeviceDataModel;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

/**
 * RabbitMQ listener for device data exchange messages
 * Processes incoming device sensor data and triggers alerts if needed
 */
@ApplicationScoped
public class DeviceDataListener {
    
    private static final Logger LOG = Logger.getLogger(DeviceDataListener.class);
    
    @Inject
    ISendMailAlertUseCase sendMailAlertUseCase;
    
    @Inject
    private ObjectMapper objectMapper;

    /**
     * Listens to device-data-distribution channel and processes device data messages
     * @param message Message containing JsonObject payload from RabbitMQ queue
     * @return Uni<Void> for reactive processing
     */
    @Incoming("device-data-distribution")
    @Blocking
    public Uni<Void> processDeviceData(Message<JsonObject> message) {
        DeviceDataModel deviceData;
        String jsonString = null;
        
        try {
            // Get JsonObject payload and convert to String
            JsonObject jsonPayload = message.getPayload();
            jsonString = jsonPayload.toString();
            LOG.infof("Received raw message: %s", jsonString);
            
            // Convert String to DeviceDataModel
            deviceData = objectMapper.readValue(jsonString, DeviceDataModel.class);
            LOG.infof("Successfully deserialized device data message for clientId: %s", deviceData.getClientId());
        } catch (Exception e) {
            LOG.errorf(e, "Failed to deserialize device data message: %s. Acknowledging and discarding.", jsonString);
            message.ack();
            return Uni.createFrom().voidItem();
        }

        try {
            // Process device data logic here
            LOG.infof("Processing device data for client: %s at timestamp: %s", 
                     deviceData.getClientId(), deviceData.getTimestamp());
            
            // Log device data details
            LOG.infof("Device data details: %s", deviceData.getData());
            
            // Example: Check for anomalies in sensor data
            if (isAnomalyDetected(deviceData)) {
                LOG.warnf("Anomaly detected for device %s at %s. Data: %s", 
                         deviceData.getClientId(), 
                         deviceData.getTimestamp(),
                         deviceData.getData());
                
                // Uncomment to send email alerts
                // sendMailAlertUseCase.sendAlert(
                //     "Device Data Anomaly Alert", 
                //     String.format("Anomaly detected for device %s at %s\nData: %s", 
                //                 deviceData.getClientId(), 
                //                 deviceData.getTimestamp(),
                //                 deviceData.getData())
                // );
            }
            
            LOG.infof("Successfully processed device data for client: %s", deviceData.getClientId());
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing device data for clientId: %s", deviceData.getClientId());
        }
        
        // Always acknowledge the message
        message.ack();
        return Uni.createFrom().voidItem();
    }
    
    /**
     * Checks if the device data indicates an anomaly
     * @param deviceData The device data to analyze
     * @return true if anomaly detected, false otherwise
     */
    private boolean isAnomalyDetected(DeviceDataModel deviceData) {
        // Implement your anomaly detection logic here
        // Example: Check for unusual sensor values
        if (deviceData.getData() != null) {
            // Add your specific anomaly detection logic
            // For now, return false to avoid false alarms
            return false;
        }
        return false;
    }
}
