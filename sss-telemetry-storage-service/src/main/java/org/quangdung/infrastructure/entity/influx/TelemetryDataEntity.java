package org.quangdung.infrastructure.entity.influx;


import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.Map;
import java.util.StringJoiner;

@Data
@Builder
public class TelemetryDataEntity {
    private String clientId;
    private String deviceUuid;
    private String deviceName;
    private String mqttUsername;
    private Instant timestamp;
    private Map<String, Object> data;

    /**
     * Converts this object into a string that complies with the InfluxDB Line Protocol syntax.
     *
     * @param measurement The name of the measurement to write the data into.
     * @return A Line Protocol string, or null if there are no valid fields.
     */
    public String toLineProtocol(String measurement) {
        // Use StringBuilder for efficient string construction
        StringBuilder line = new StringBuilder();

        // --- 1. Append Measurement and Tags ---
        line.append(measurement);

        // Append tags, separated by commas
        appendTag(line, "clientId", this.clientId);
        appendTag(line, "deviceUuid", this.deviceUuid);
        appendTag(line, "deviceName", this.deviceName);
        appendTag(line, "mqttUsername", this.mqttUsername);

        // Add a space to separate tags from fields
        line.append(" ");

        // --- 2. Append Fields ---
        // Use StringJoiner to handle commas between fields
        StringJoiner fieldJoiner = new StringJoiner(",");
        if (this.data != null && !this.data.isEmpty()) {
            for (Map.Entry<String, Object> entry : this.data.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (value instanceof String) {
                    // String values must be enclosed in double quotes
                    fieldJoiner.add(key + "=\"" + value + "\"");
                } else if (value instanceof Number || value instanceof Boolean) {
                    // Numbers and Booleans do not need double quotes
                    fieldJoiner.add(key + "=" + value);
                }
            }
        }
        
        // InfluxDB requires at least one field, otherwise the point is invalid.
        if (fieldJoiner.length() == 0) {
            return null; // or throw an exception
        }

        line.append(fieldJoiner);

        // --- 3. Append Timestamp ---
        // Add a space to separate fields from the timestamp
        line.append(" ");
        // The timestamp must be in nanosecond precision
        long timestampNanos = this.timestamp.getEpochSecond() * 1_000_000_000L + this.timestamp.getNano();
        line.append(timestampNanos);

        return line.toString();
    }

    /**
     * Helper method to append a tag to the string builder.
     */
    private void appendTag(StringBuilder builder, String key, String value) {
        // Only append the tag if the value is not null or empty
        if (value != null && !value.isEmpty()) {
            builder.append(",").append(key).append("=").append(value);
        }
    }
}
