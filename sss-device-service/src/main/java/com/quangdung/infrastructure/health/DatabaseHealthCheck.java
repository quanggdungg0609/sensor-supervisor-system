package com.quangdung.infrastructure.health;

import io.smallrye.mutiny.Uni;
import io.vertx.mutiny.pgclient.PgPool;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Readiness;

/**
 * Custom health check for database connectivity
 * Verifies that the PostgreSQL database is accessible and responsive
 */
@Readiness
@ApplicationScoped
public class DatabaseHealthCheck implements HealthCheck {

    @Inject
    PgPool pgPool;

    /**
     * Performs database health check by executing a simple query
     * @return HealthCheckResponse indicating database status
     */
    @Override
    public HealthCheckResponse call() {
        try {
            // Thực hiện query đơn giản để kiểm tra kết nối
            pgPool.query("SELECT 1").execute().await().indefinitely();
            
            return HealthCheckResponse.named("database")
                    .up()
                    .withData("connection", "active")
                    .withData("database", "postgresql")
                    .build();
        } catch (Exception e) {
            return HealthCheckResponse.named("database")
                    .down()
                    .withData("connection", "failed")
                    .withData("error", e.getMessage())
                    .build();
        }
    }
}