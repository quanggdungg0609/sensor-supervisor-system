package org.quangdung.application.rabbitmq_listerner;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.quangdung.domain.model.DeviceDataModel;
import org.quangdung.domain.model.ThresholdKey;
import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.usecase.interfaces.ICheckThresholdUseCase;
import org.quangdung.domain.usecase.interfaces.IGetAllThresholdsUseCase;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import org.quangdung.domain.usecase.interfaces.IGetDeviceByClientIdUseCase;
import io.smallrye.common.annotation.Blocking;
import org.jboss.logging.Logger;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;

import java.util.HashMap;
import java.util.Map;

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
    
    @Inject
    private ICheckThresholdUseCase checkThresholdUseCase;
    
    @Inject
    private IGetAllThresholdsUseCase getAllThresholdsUseCase;
    
    @Inject
    private IGetDeviceByClientIdUseCase getDeviceByClientIdUseCase;

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
            
            // First get all thresholds to know which values to extract
            return getAllThresholdsUseCase.execute(deviceData.getClientId())
                .onItem().transformToUni(thresholds -> {
                    // Extract only values that have corresponding thresholds
                    Map<String, Double> valuesToCheck = extractRelevantSensorValues(deviceData, thresholds);
                    
                    if (valuesToCheck.isEmpty()) {
                        LOG.infof("No relevant values to check against thresholds for client %s", deviceData.getClientId());
                        message.ack();
                        return Uni.createFrom().voidItem();
                    }
                    
                    // Check if values exceed thresholds
                    return checkThresholdUseCase.execute(deviceData.getClientId(), valuesToCheck)
                        .onItem().invoke(threshold -> {
                            if (threshold != null) {
                                LOG.warnf("Threshold exceeded for client %s: %s - %s. Values: %s", 
                                    deviceData.getClientId(),
                                    threshold.getThresholdLevel(),
                                    threshold.getThresholdType(),
                                    valuesToCheck);
                                LOG.warnf("Threshold message: %s", threshold.getMessage());
                                LOG.warnf("Threshold values: %s", threshold.getThreshold());
                                
                                // Send email alert when threshold is not null
                                getDeviceByClientIdUseCase.execute(deviceData.getClientId())
                                    .subscribe().with(
                                        deviceInfo -> {
                                            if (deviceInfo != null) {
                                                sendMailAlertUseCase.execute(valuesToCheck, threshold, deviceInfo)
                                                    .subscribe().with(
                                                        success -> LOG.infof("Alert email sent successfully for client %s", deviceData.getClientId()),
                                                        error -> LOG.errorf(error, "Failed to send alert email for client %s", deviceData.getClientId())
                                                    );
                                            } else {
                                                LOG.warnf("Could not find device info for client %s", deviceData.getClientId());
                                            }
                                        },
                                        error -> LOG.errorf(error, "Error getting device info for client %s", deviceData.getClientId())
                                    );
                            } else {
                                LOG.infof("No thresholds exceeded for client %s. Values: %s", 
                                    deviceData.getClientId(), valuesToCheck);
                            }
                        })
                        .onFailure().invoke(error -> 
                            LOG.errorf(error, "Error checking thresholds for clientId: %s", deviceData.getClientId())
                        )
                        .onItem().ignore().andContinueWithNull();
                })
                .onFailure().recoverWithItem(() -> {
                    LOG.errorf("Failed to get thresholds for clientId: %s", deviceData.getClientId());
                    return null;
                })
                .onItem().invoke(v -> {
                    LOG.infof("Successfully processed device data for client: %s", deviceData.getClientId());
                    message.ack();
                })
                .onItem().ignore().andContinueWithNull();
            
        } catch (Exception e) {
            LOG.errorf(e, "Error processing device data for clientId: %s", deviceData.getClientId());
            message.ack();
            return Uni.createFrom().voidItem();
        }
    }
    
    /**
     * Extracts only sensor values that have corresponding thresholds defined
     * 
     * @param deviceData The device data containing sensor readings
     * @param thresholds Map of threshold models to check against
     * @return Map containing only values that have corresponding thresholds
     */
    private Map<String, Double> extractRelevantSensorValues(DeviceDataModel deviceData, Map<ThresholdKey, ThresholdModel> thresholds) {
        Map<String, Double> values = new HashMap<>();
        
        if (deviceData.getData() == null || thresholds == null || thresholds.isEmpty()) {
            return values;
        }
        
        // Get all threshold keys from all threshold models
        for (ThresholdModel threshold : thresholds.values()) {
            Map<String, Double> thresholdValues = threshold.getThreshold();
            if (thresholdValues == null) {
                continue;
            }
            
            // For each key in the threshold
            for (String key : thresholdValues.keySet()) {
                // Skip power_status explicitly
                if ("power_status".equals(key)) {
                    continue;
                }
                
                // If we already extracted this value, skip
                if (values.containsKey(key)) {
                    continue;
                }
                
                // If the device data contains this key, extract it
                if (deviceData.getData().containsKey(key)) {
                    Object value = deviceData.getData().get(key);
                    if (value != null) {
                        if (value instanceof Number) {
                            values.put(key, ((Number) value).doubleValue());
                        } else if (value instanceof String) {
                            try {
                                values.put(key, Double.parseDouble((String) value));
                            } catch (NumberFormatException e) {
                                LOG.warnf("Could not parse %s value: %s", key, value);
                            }
                        }
                    }
                }
            }
        }
        
        return values;
    }

}
