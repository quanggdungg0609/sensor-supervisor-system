#ifndef SENSOR_MANAGER_HPP
#define SENSOR_MANAGER_HPP

#include <string>
#include "sht30_sensor.hpp"
#include "power_outage_detector.hpp"
// #include "pt100_sensor.hpp"  // Comment out PT100

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
     * @brief Get temperature from SHT30 sensor
     * @return Temperature in Celsius
     */
    float getTemperature();
    
    /**
     * @brief Get humidity from SHT30 sensor
     * @return Humidity in percentage
     */
    float getHumidity();
    
    /**
     * @brief Disable power outage wake-up configuration
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t disablePowerOutageWakeUp();

private:
    SHT30Sensor sht30Sensor;                    ///< SHT30 temperature and humidity sensor
    PowerOutageDetector powerOutageDetector;    ///< Power outage detector
    // PT100Sensor pt100;                       // Comment out PT100
};

#endif // SENSOR_MANAGER_HPP