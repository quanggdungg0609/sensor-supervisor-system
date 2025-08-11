package org.quangdung.health;

import io.smallrye.health.HealthStatus;
import io.smallrye.health.checks.UrlHealthCheck;
import org.eclipse.microprofile.health.HealthCheck;
import org.eclipse.microprofile.health.HealthCheckResponse;
import org.eclipse.microprofile.health.Liveness;
import org.eclipse.microprofile.health.Readiness;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import io.vertx.mutiny.pgclient.PgPool;
import io.smallrye.mutiny.Uni;

/**
 * Database health check implementation
 * Checks PostgreSQL database connectivity for both liveness and readiness
 */
@ApplicationScoped
@Liveness
@Readiness
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
            // Execute simple query to check database connectivity
            return pgPool.query("SELECT 1")
                    .execute()
                    .map(rows -> HealthCheckResponse.up("Database connection healthy"))
                    .onFailure()
                    .recoverWithItem(throwable -> 
                        HealthCheckResponse.down("Database connection failed: " + throwable.getMessage())
                    )
                    .await().indefinitely();
        } catch (Exception e) {
            return HealthCheckResponse.down("Database health check failed: " + e.getMessage());
        }
    }
}