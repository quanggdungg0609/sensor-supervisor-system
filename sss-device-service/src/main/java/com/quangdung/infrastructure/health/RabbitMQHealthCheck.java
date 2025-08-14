package com.quangdung.infrastructure.health;

import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

import java.net.Socket;

/**
 * Custom health check for RabbitMQ connectivity
 * Verifies that RabbitMQ broker is accessible
 */
@Readiness
@ApplicationScoped
public class RabbitMQHealthCheck implements HealthCheck {

    @ConfigProperty(name = "mp.messaging.connector.rabbitmq.host", defaultValue = "localhost")
    String rabbitmqHost;

    @ConfigProperty(name = "mp.messaging.connector.rabbitmq.port", defaultValue = "5672")
    int rabbitmqPort;

    /**
     * Performs RabbitMQ health check by testing socket connection
     * @return HealthCheckResponse indicating RabbitMQ status
     */
    @Override
    public HealthCheckResponse call() {
        try (Socket socket = new Socket(rabbitmqHost, rabbitmqPort)) {
            return HealthCheckResponse.named("rabbitmq")
                    .up()
                    .withData("host", rabbitmqHost)
                    .withData("port", rabbitmqPort)
                    .withData("connection", "active")
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("rabbitmq")
                    .down()
                    .withData("host", rabbitmqHost)
                    .withData("port", rabbitmqPort)
                    .withData("connection", "failed")
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}