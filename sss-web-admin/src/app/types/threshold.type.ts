/**
 * Threshold related type definitions
 */

/**
 * Threshold level enum
 */
export type ThresholdLevel = 'WARNING' | 'CRITICAL';

/**
 * Threshold type enum
 */
export type ThresholdType = 'UPPER' | 'LOWER';

/**
 * Dynamic threshold values interface
 * @interface ThresholdValues
 * @property {Record<string, number>} [key] - Dynamic telemetry values (e.g., temperature, humidity)
 */
export interface ThresholdValues {
  [key: string]: number;
}

/**
 * Telemetry values response interface
 * @interface TelemetryValuesResponse
 * @property {string[]} telemetry_values - Array of available telemetry value names
 */
export interface TelemetryValuesResponse {
  telemetry_values: string[];
}

/**
 * Threshold configuration request interface
 * @interface ThresholdConfigRequest
 * @property {ThresholdLevel} thresholdLevel - Level of the threshold (WARNING or CRITICAL)
 * @property {ThresholdType} thresholdType - Type of threshold (UPPER or LOWER)
 * @property {string | null} message - Optional message for the threshold
 * @property {ThresholdValues} threshold - Dynamic threshold values for available telemetry
 */
export interface ThresholdConfigRequest {
  thresholdLevel: ThresholdLevel;
  thresholdType: ThresholdType;
  message: string | null;
  threshold: ThresholdValues;
}

/**
 * Threshold configuration response interface
 * @interface ThresholdConfigResponse
 * @property {string} status - Response status
 * @property {string} message - Response message
 */
export interface ThresholdConfigResponse {
  status: string;
  message: string;
}

/**
 * Threshold detail interface
 * @interface ThresholdDetail
 * @property {string} clientId - Client ID associated with the device
 * @property {ThresholdLevel} thresholdLevel - Level of the threshold (WARNING or CRITICAL)
 * @property {ThresholdType} thresholdType - Type of threshold (UPPER or LOWER)
 * @property {string | null} message - Optional message for the threshold
 * @property {ThresholdValues} threshold - Dynamic threshold values for telemetry
 */
export interface ThresholdDetail {
  clientId: string;
  thresholdLevel: ThresholdLevel;
  thresholdType: ThresholdType;
  message: string | null;
  threshold: ThresholdValues;
}

/**
 * All thresholds response interface
 * @interface AllThresholdsResponse
 * @property {Record<string, ThresholdDetail>} - Key-value pairs where keys are threshold identifiers
 */
export interface AllThresholdsResponse {
  [key: string]: ThresholdDetail;
}