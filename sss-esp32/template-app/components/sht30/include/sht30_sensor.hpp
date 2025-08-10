#ifndef SHT30_SENSOR_HPP
#define SHT30_SENSOR_HPP

#include "esp_log.h"
#include "driver/i2c_master.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cmath>

/**
 * @brief SHT30 Temperature and Humidity Sensor Class
 * 
 * This class provides direct communication with SHT30 sensor using
 * the new i2c_master API from ESP-IDF v5.0+
 */
class SHT30Sensor {
public:
    /**
     * @brief Default constructor for SHT30Sensor
     */
    SHT30Sensor();
    
    /**
     * @brief Destructor for SHT30Sensor
     */
    ~SHT30Sensor();
    
    /**
     * @brief Initialize the SHT30 sensor with specified GPIO pins
     * @param sda_pin GPIO pin for SDA (default: 21)
     * @param scl_pin GPIO pin for SCL (default: 22)
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t init(int sda_pin = 21, int scl_pin = 22);
    
    /**
     * @brief Read temperature and humidity from sensor
     * @param temperature Pointer to store temperature value (°C)
     * @param humidity Pointer to store humidity value (%RH)
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t readTempHumidity(float* temperature, float* humidity);
    
    /**
     * @brief Get temperature reading
     * @return Temperature in Celsius, NaN if error
     */
    float getTemperature();
    
    /**
     * @brief Get humidity reading
     * @return Humidity in %RH, NaN if error
     */
    float getHumidity();
    
    /**
     * @brief Check if sensor is available
     * @return true if sensor responds, false otherwise
     */
    bool isAvailable();
    
private:
    static const char* TAG;                    ///< Log tag
    static const uint8_t SHT30_ADDR = 0x44;   ///< SHT30 I2C address
    static const uint32_t I2C_FREQ_HZ = 100000; ///< I2C frequency
    
    i2c_master_bus_handle_t m_bus_handle;     ///< I2C master bus handle
    i2c_master_dev_handle_t m_dev_handle;     ///< I2C device handle
    bool m_initialized;                       ///< Initialization status
    
    /**
     * @brief Send measurement command to sensor
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t sendMeasurementCommand();
    
    /**
     * @brief Read raw data from sensor
     * @param data Buffer to store 6 bytes of data
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t readRawData(uint8_t* data);
};

#endif // SHT30_SENSOR_HPP