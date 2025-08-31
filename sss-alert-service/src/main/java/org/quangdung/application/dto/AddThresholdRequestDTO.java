package org.quangdung.application.dto;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddThresholdRequestDTO {
    private String thresholdLevel;
    private String thresholdType; 
    private String message;
    private Map<String, Double> threshold;
}
 