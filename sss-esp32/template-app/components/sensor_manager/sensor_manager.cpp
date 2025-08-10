#include "sensor_manager.hpp"
#include "ntp_client.hpp"
#include "ArduinoJson.h"
#include "esp_log.h"
#include <cmath>

static const char* TAG = "SENSOR_MANAGER";

/**
 * @brief Initialize all sensors in the system
 * @return true if all sensors initialized successfully, false otherwise
 */
bool SensorManager::init() {
    ESP_LOGI(TAG, "Initializing sensor manager...");
    
    // Initialize SHT30 sensor with default pins (SDA=21, SCL=22)
    esp_err_t ret = sht30Sensor.init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to initialize SHT30 sensor: %s", esp_err_to_name(ret));
        return false;
    }
    
    // Initialize Power Outage Detector
    ret = powerOutageDetector.init();
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to initialize power outage detector: %s", esp_err_to_name(ret));
        return false;
    }
    
    ESP_LOGI(TAG, "Sensor manager initialized successfully with SHT30 and Power Outage Detector");
    return true;
}

/**
 * @brief Get aggregated sensor data in JSON format
 * @return JSON string containing sensor data with timestamp
 */
std::string SensorManager::getAggregatedDataJson() {
    JsonDocument doc;
    doc["timestamp"] = NTPClient::getInstance().getFormattedTimestamp();
    
    JsonObject data = doc["data"].to<JsonObject>();
    data["temperature"] = getTemperature();
    data["humidity"] = getHumidity();
    data["power_status"] = getPowerStatus();  // Add power status field
    
    std::string jsonString;
    serializeJson(doc, jsonString);
    return jsonString;
}

/**
 * @brief Get power outage status for emergency alerts
 * @return JSON string with power status only
 */
std::string SensorManager::getPowerOutageJson() {
    JsonDocument doc;
    doc["timestamp"] = NTPClient::getInstance().getFormattedTimestamp();
    
    JsonObject data = doc["data"].to<JsonObject>();
    data["power_status"] = getPowerStatus();
    
    std::string jsonString;
    serializeJson(doc, jsonString);
    return jsonString;
}

/**
 * @brief Get temperature from SHT30 sensor
 * @return Temperature in Celsius, NAN if error
 */
float SensorManager::getTemperature() {
    return sht30Sensor.getTemperature();
}

/**
 * @brief Get humidity from SHT30 sensor
 * @return Humidity percentage, NAN if error
 */
float SensorManager::getHumidity() {
    return sht30Sensor.getHumidity();
}

/**
 * @brief Get power status from power outage detector
 * @return 1 if power available, 0 if power outage
 */
int SensorManager::getPowerStatus() {
    return powerOutageDetector.getPowerStatus();
}

/**
 * @brief Configure power outage wake-up for deep sleep
 */
void SensorManager::configurePowerOutageWakeUp() {
    powerOutageDetector.configureWakeUp();
}

/**
 * @brief Disable power outage wake-up configuration
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t SensorManager::disablePowerOutageWakeUp() {
    return powerOutageDetector.disableWakeUp();
}
