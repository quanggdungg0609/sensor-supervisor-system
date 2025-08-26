package org.quangdung.infrastructure.entity;

import java.util.Map;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ThresholdEntity {
    private String clientId;
    private Map<String, Double> threshold;
}
