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
    
    // Initialize PT100 sensor
    if (!pt100Sensor.init()) {
        ESP_LOGE(TAG, "Failed to initialize PT100 sensor");
        return false;
    }
    
    // Initialize Power Outage Detector
    esp_err_t ret = powerOutageDetector.init(GPIO_NUM_2);  // Thêm tham số GPIO_NUM_2
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to initialize power outage detector: %s", esp_err_to_name(ret));
        return false;
    }
    
    ESP_LOGI(TAG, "Sensor manager initialized successfully with PT100 and Power Outage Detector");
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
    // Removed humidity field since PT100 only measures temperature
    data["power_status"] = getPowerStatus();
    
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
 * @brief Get temperature from PT100 sensor
 * @return Temperature in Celsius, NAN if error
 */
float SensorManager::getTemperature() {
    return pt100Sensor.readTemperature();
}

/**
 * @brief Get humidity (not available with PT100)
 * @return NAN since PT100 only measures temperature
 */
float SensorManager::getHumidity() {
    ESP_LOGW(TAG, "Humidity not available with PT100 sensor");
    return NAN;
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
