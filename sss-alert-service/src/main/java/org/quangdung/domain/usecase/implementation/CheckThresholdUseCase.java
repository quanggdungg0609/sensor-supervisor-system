package org.quangdung.domain.usecase.implementation;

import java.util.Map;
import java.util.Optional;
import java.util.Comparator;

import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.usecase.interfaces.ICheckThresholdUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CheckThresholdUseCase implements ICheckThresholdUseCase {
    @Inject
    private GetAllThresholdsUseCase getAllThresholdsUseCase;

    /**
     * Checks if any of the provided values exceed the thresholds for the client
     * and returns the highest priority threshold that was exceeded.
     * 
     * @param clientId The client ID to check thresholds for
     * @param values The current values to check against thresholds
     * @return Uni<ThresholdModel> containing the highest priority threshold that was exceeded, or null if no thresholds were exceeded
     */
    @Override
    public Uni<ThresholdModel> execute(String clientId, Map<String, Double> values) {
        return getAllThresholdsUseCase.execute(clientId)
            .onItem().transform(thresholds -> {
                // Find all thresholds that are exceeded
                Optional<ThresholdModel> highestPriorityThreshold = thresholds.values().stream()
                    .filter(threshold -> isThresholdExceeded(threshold, values))
                    // Sort by priority (CRITICAL is higher priority than WARNING)
                    .sorted(Comparator.comparing(ThresholdModel::getThresholdLevel))
                    .findFirst();
                
                return highestPriorityThreshold.orElse(null);
            });
    }
    
    /**
     * Checks if any of the provided values exceed the threshold
     * 
     * @param threshold The threshold model to check against
     * @param currentValues The current values to check
     * @return true if any value exceeds the threshold, false otherwise
     */
    private boolean isThresholdExceeded(ThresholdModel threshold, Map<String, Double> currentValues) {
        Map<String, Double> thresholdValues = threshold.getThreshold();
        
        // Check each key in the threshold
        for (Map.Entry<String, Double> entry : thresholdValues.entrySet()) {
            String key = entry.getKey();
            Double thresholdValue = entry.getValue();
            
            // Skip if the current values don't contain this key
            if (!currentValues.containsKey(key)) {
                continue;
            }
            
            Double currentValue = currentValues.get(key);
            
            // Check if the value exceeds the threshold based on threshold type
            if (threshold.getThresholdType() == ThresholdModel.ThresholdType.UPPER) {
                // For UPPER type, current value should be less than or equal to threshold
                if (currentValue > thresholdValue) {
                    return true;
                }
            } else if (threshold.getThresholdType() == ThresholdModel.ThresholdType.LOWER) {
                // For LOWER type, current value should be greater than or equal to threshold
                if (currentValue < thresholdValue) {
                    return true;
                }
            }
        }
        
        return false;
    }
}
