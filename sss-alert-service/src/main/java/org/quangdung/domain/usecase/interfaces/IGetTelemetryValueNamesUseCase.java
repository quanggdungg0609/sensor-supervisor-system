package org.quangdung.domain.usecase.interfaces;

import java.util.List;

import io.smallrye.mutiny.Multi;

/**
 * Interface for retrieving telemetry value names for a specific client
 */
public interface IGetTelemetryValueNamesUseCase {
    /**
     * Retrieves all telemetry field names for a specific device
     * 
     * @param deviceUuid The device UUID to retrieve field names for
     * @return Multi<List<String>> A stream of lists containing field names
     */
    Multi<List<String>> execute(String deviceUuid);
}