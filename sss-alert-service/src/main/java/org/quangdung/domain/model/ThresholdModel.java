package org.quangdung.domain.model;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThresholdModel {
    private String clientId;
    private Map<String, Double> threshold;
}
