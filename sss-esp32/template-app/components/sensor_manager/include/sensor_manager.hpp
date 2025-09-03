#ifndef SENSOR_MANAGER_HPP
#define SENSOR_MANAGER_HPP

#include <string>
#include "pt100_sensor.hpp"
#include "power_outage_detector.hpp"

class SensorManager {
public:
    SensorManager() = default;
    
    /**
     * @brief Initialize all sensors
     * @return true if all sensors initialized successfully
     */
    bool init();
    
    /**
     * @brief Get aggregated sensor data in JSON format
     * @return JSON string containing sensor data with timestamp
     */
    std::string getAggregatedDataJson();
    
    /**
     * @brief Get power outage status for emergency alerts
     * @return JSON string with power status only
     */
    std::string getPowerOutageJson();
    
    /**
     * @brief Configure power outage wake-up for deep sleep
     */
    void configurePowerOutageWakeUp();
    
    /**
     * @brief Get power status from power outage detector
     * @return 1 if power available, 0 if power outage
     */
    int getPowerStatus();
    
    /**
     * @brief Get temperature from PT100 sensor
     * @return Temperature in Celsius
     */
    float getTemperature();
    
    /**
     * @brief Get humidity (deprecated - PT100 only measures temperature)
     * @return NAN since PT100 only measures temperature
     */
    float getHumidity();
    
    /**
     * @brief Disable power outage wake-up configuration
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t disablePowerOutageWakeUp();

private:
    PT100Sensor pt100Sensor;                   ///< PT100 temperature sensor
    PowerOutageDetector powerOutageDetector;    ///< Power outage detector
};

#endif // SENSOR_MANAGER_HPP