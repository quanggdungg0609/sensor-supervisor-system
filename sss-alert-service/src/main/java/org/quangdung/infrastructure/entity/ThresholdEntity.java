package org.quangdung.infrastructure.entity;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThresholdEntity {
    private String clientId;
    private ThresholdLevel thresholdLevel;   
    private ThresholdType thresholdType; // Thêm trường này
    private String message; 
    private Map<String, Double> threshold;

    public enum ThresholdLevel {
        CRITICAL,
        WARNING
    }
    
    public enum ThresholdType {
        UPPER,
        LOWER
    }
}
