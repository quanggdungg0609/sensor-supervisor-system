package org.quangdung.domain.usecase.implementation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jboss.logging.Logger;
import org.quangdung.domain.model.DeviceInfoModel;
import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.usecase.interfaces.IGetAlertEmailsUseCase;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;
import org.quangdung.infrastructure.component.mail.MailComponent;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class SendMailAlertUseCase implements ISendMailAlertUseCase {

    @Inject
    private Logger log;

    @Inject
    private MailComponent mailComponent;

    @Inject
    private IGetAlertEmailsUseCase getAlertEmailsUseCase;


    private String subjectTemplate = "[{threshold_level}] Alert for {device_name}";

    /**
     * Enum containing all available placeholders for message templates.
     * This makes it easy to add new placeholders in the future.
     */
    public enum MessagePlaceholder {
        THRESHOLD_NAME("{threshold_name}"),
        CURRENT_VALUE("{current_value}"),
        THRESHOLD_VALUE("{threshold_value}"),
        DEVICE_NAME("{device_name}"),
        DEVICE_UUID("{device_uuid}"),
        CLIENT_ID("{client_id}"),
        THRESHOLD_LEVEL("{threshold_level}"),
        THRESHOLD_TYPE("{threshold_type}");

        private final String placeholder;

        MessagePlaceholder(String placeholder) {
            this.placeholder = placeholder;
        }

        public String getPlaceholder() {
            return placeholder;
        }

        /**
         * Get a placeholder by its string value.
         *
         * @param value The string value of the placeholder
         * @return The corresponding MessagePlaceholder or null if not found
         */
        public static MessagePlaceholder fromString(String value) {
            for (MessagePlaceholder placeholder : MessagePlaceholder.values()) {
                if (placeholder.getPlaceholder().equals(value)) {
                    return placeholder;
                }
            }
            return null;
        }
    }

    @Override
    public Uni<Void> execute(Map<String, Double> currentValues, ThresholdModel threshold, DeviceInfoModel deviceInfo) {
        // Process subject template
        String subject = processSubjectTemplate(threshold, deviceInfo);
        
        // Create header message only once
        StringBuilder alertMessage = new StringBuilder();
        
        // Add device and threshold information only once
        String headerTemplate = "Device [{device_name}] avec CliendID: {client_id} est violé la seuil.\n" +
                              "Niveau de threshold: {threshold_level}\n" +
                              "Type de threshold: {threshold_type}";
        
        // Create and replace placeholders in header
        Set<String> headerPlaceholders = extractPlaceholders(headerTemplate);
        
        // Use createPlaceholderValues with empty sensorName and 0.0 values as they are not needed for header
        Map<String, String> headerValues = createPlaceholderValues("", 0.0, 0.0, threshold, deviceInfo);
        
        String header = replacePlaceholders(headerTemplate, headerValues, headerPlaceholders);
        alertMessage.append(header);
        
        // Template for each violation
        String violationTemplate = "\nLa {threshold_name} est: {current_value}. Il a dépassé {threshold_value}";
        Set<String> violationPlaceholders = extractPlaceholders(violationTemplate);
        
        // Check and add message for each violation
        int violationCount = 0;
        
        for (Map.Entry<String, Double> entry : currentValues.entrySet()) {
            String sensorName = entry.getKey();
            Double currentValue = entry.getValue();
            Double thresholdValue = threshold.getThreshold().get(sensorName);
            
            if (thresholdValue == null) {
                continue;
            }
            
            boolean isViolated = false;
            if (threshold.getThresholdType() == ThresholdModel.ThresholdType.UPPER && currentValue >= thresholdValue) {
                isViolated = true;
            } else if (threshold.getThresholdType() == ThresholdModel.ThresholdType.LOWER && currentValue <= thresholdValue) {
                isViolated = true;
            }
            
            if (isViolated) {
                violationCount++;
                
                // Use createPlaceholderValues to create map for violation placeholders
                Map<String, String> violationValues = createPlaceholderValues(sensorName, currentValue, thresholdValue, threshold, deviceInfo);
                
                // Replace placeholders and add to message
                String violationMessage = replacePlaceholders(violationTemplate, violationValues, violationPlaceholders);
                alertMessage.append(violationMessage);
            }
        }
        
        // Log the message if there are violations
        if (violationCount > 0) {
            // Add device information and alert level at the beginning of the message
            // String deviceInfoString = String.format("Device: %s (ClientID: %s)", 
            //         deviceInfo.getDeviceName(), deviceInfo.getClientId());
            // String alertLevel = "Alert Level: " + threshold.getThresholdLevel();
            String fullMessage =  alertMessage.toString();
            
            log.info("Subject: " + subject);
            log.info("ALERT DETECTED:\n" + fullMessage);
            return getAlertEmailsUseCase.execute()
            .onItem().transformToUni(emails -> {
                if (emails != null && !emails.isEmpty()) {
                    log.info("Sending alert email to " + emails.size() + " recipients");
                    //  return Uni.createFrom().voidItem(); 

                    return mailComponent.sendAlertMail(subject, fullMessage, emails);
                } else {
                    log.info("No alert email recipients configured, skipping email notification");
                    return Uni.createFrom().voidItem();
                }
            });
            // mailComponent.sendMail(subject, fullMessage);
            // Uncomment the line above if you want to send email
        }
        
        return Uni.createFrom().voidItem(); 
    }

    public Uni<Void> execute(String subject, String body) {
        return getAlertEmailsUseCase.execute()
        .onItem().transformToUni(emails -> {
            if (emails != null && !emails.isEmpty()) {
                log.info("Sending alert email to " + emails.size() + " recipients");
                //  return Uni.createFrom().voidItem(); 

                return mailComponent.sendAlertMail(subject, body, emails);
            } else {
                log.info("No alert email recipients configured, skipping email notification");
                return Uni.createFrom().voidItem();
            }
        });
    }
    
    /**
     * Process the subject template by replacing placeholders with actual values.
     *
     * @param threshold The threshold model containing threshold information
     * @param deviceInfo Information about the device
     * @return The processed subject string with placeholders replaced
     */
    private String processSubjectTemplate(ThresholdModel threshold, DeviceInfoModel deviceInfo) {
        Set<String> subjectPlaceholders = extractPlaceholders(subjectTemplate);
        Map<String, String> placeholderValues = createPlaceholderValues("", 0.0, 0.0, threshold, deviceInfo);
        return replacePlaceholders(subjectTemplate, placeholderValues, subjectPlaceholders);
    }

    /**
     * Extracts all placeholders from a template string.
     * Placeholders are identified as text surrounded by curly braces, e.g., {placeholder}.
     *
     * @param template The template string to extract placeholders from
     * @return A set of all placeholders found in the template
     */
    private Set<String> extractPlaceholders(String template) {
        Set<String> placeholders = new HashSet<>();
        Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
        Matcher matcher = pattern.matcher(template);
        
        while (matcher.find()) {
            placeholders.add("{" + matcher.group(1) + "}");
        }
        
        return placeholders;
    }

    /**
     * Creates a map of placeholder values based on the current alert context.
     *
     * @param sensorName The name of the sensor that triggered the alert
     * @param currentValue The current value of the sensor
     * @param thresholdValue The threshold value that was exceeded
     * @param threshold The threshold model containing threshold information
     * @param deviceInfo Information about the device
     * @return A map containing all possible placeholder values
     */
    private Map<String, String> createPlaceholderValues(String sensorName, Double currentValue, 
            Double thresholdValue, ThresholdModel threshold, DeviceInfoModel deviceInfo) {
        Map<String, String> values = new HashMap<>();
        
        // Add all possible placeholder values
        values.put(MessagePlaceholder.THRESHOLD_NAME.getPlaceholder(), sensorName);
        values.put(MessagePlaceholder.CURRENT_VALUE.getPlaceholder(), String.valueOf(currentValue));
        values.put(MessagePlaceholder.THRESHOLD_VALUE.getPlaceholder(), String.valueOf(thresholdValue));
        values.put(MessagePlaceholder.DEVICE_NAME.getPlaceholder(), deviceInfo.getDeviceName());
        values.put(MessagePlaceholder.DEVICE_UUID.getPlaceholder(), deviceInfo.getDeviceUuid());
        values.put(MessagePlaceholder.CLIENT_ID.getPlaceholder(), deviceInfo.getClientId());
        values.put(MessagePlaceholder.THRESHOLD_LEVEL.getPlaceholder(), threshold.getThresholdLevel().toString());
        values.put(MessagePlaceholder.THRESHOLD_TYPE.getPlaceholder(), threshold.getThresholdType().toString());
        
        return values;
    }

    /**
     * Replaces placeholders in a template with their corresponding values.
     * Only replaces placeholders that are actually present in the template.
     *
     * @param template The template string containing placeholders
     * @param placeholderValues A map of all available placeholder values
     * @param placeholdersInTemplate The set of placeholders actually present in the template
     * @return The template with placeholders replaced by their values
     */
    private String replacePlaceholders(String template, Map<String, String> placeholderValues, 
            Set<String> placeholdersInTemplate) {
        String result = template;
        
        // Only replace placeholders that are actually in the template
        for (String placeholder : placeholdersInTemplate) {
            String value = placeholderValues.get(placeholder);
            if (value != null) {
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
}
