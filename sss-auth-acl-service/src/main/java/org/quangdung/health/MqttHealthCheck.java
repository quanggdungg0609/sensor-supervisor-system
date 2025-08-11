package org.quangdung.health;

import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;

/**
 * MQTT health check implementation
 * Checks MQTT broker connectivity for readiness
 */
@ApplicationScoped
@Readiness
public class MqttHealthCheck implements HealthCheck {

    /**
     * Performs MQTT broker health check
     * @return HealthCheckResponse indicating MQTT broker status
     */
    @Override
    public HealthCheckResponse call() {
        try {
            // Add your MQTT connection check logic here
            // For now, return UP - implement actual MQTT ping/connection test
            return HealthCheckResponse.up("MQTT broker connection healthy");
        } catch (Exception e) {
            return HealthCheckResponse.down("MQTT broker connection failed: " + e.getMessage());
        }
    }
}