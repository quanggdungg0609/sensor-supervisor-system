package org.quangdung.domain.model;

import org.quangdung.domain.model.ThresholdModel.ThresholdLevel;
import org.quangdung.domain.model.ThresholdModel.ThresholdType;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Composite key for threshold maps, combining threshold level and type
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ThresholdKey {
    private ThresholdLevel level;
    private ThresholdType type;
    
    @Override
    public String toString() {
        return level + "-" + type;
    }
}