package org.quangdung.domain.model;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThresholdModel {
    private String clientId;
    private ThresholdLevel thresholdLevel;
    private ThresholdType thresholdType; 
    private String message;
    private Map<String, Double> threshold;

    public enum ThresholdLevel{
        CRITICAL,
        WARNING,
    }
    
    public enum ThresholdType {
        UPPER,
        LOWER
    }
}
