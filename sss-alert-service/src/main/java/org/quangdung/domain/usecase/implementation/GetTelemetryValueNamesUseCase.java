package org.quangdung.domain.usecase.implementation;

import java.util.List;

import org.quangdung.domain.usecase.interfaces.IGetTelemetryValueNamesUseCase;
import org.quangdung.infrastructure.component.influx_component.InfluxComponent;

import io.smallrye.mutiny.Multi;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

/**
 * Implementation of IGetTelemetryValueNamesUseCase for retrieving telemetry value names
 */
@ApplicationScoped
public class GetTelemetryValueNamesUseCase implements IGetTelemetryValueNamesUseCase {
    
    @Inject
    private InfluxComponent influxComponent;

    /**
     * Retrieves all telemetry field names for a specific device
     * 
     * @param deviceUuid The device UUID to retrieve field names for
     * @return Multi<List<String>> A stream of lists containing field names
     */
    @Override
    public Multi<List<String>> execute(String deviceUuid) {
        return influxComponent.getTelemetryValueNames(deviceUuid);
    }
}