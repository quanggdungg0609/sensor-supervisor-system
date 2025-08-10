#ifndef PT100_SENSOR_HPP
#define PT100_SENSOR_HPP

#include "max31865.h"
#include "esp_err.h"

/**
 * @brief PT100 temperature sensor class using MAX31865 amplifier
 */
class PT100Sensor {
public:
    /**
     * @brief Default constructor
     */
    PT100Sensor() = default;
    
    /**
     * @brief Initialize the PT100 sensor with MAX31865
     * @return true if initialization successful, false otherwise
     */
    bool init();
    
    /**
     * @brief Read temperature from PT100 sensor
     * @return Temperature in Celsius, NAN if error occurred
     */
    float readTemperature();
    
    /**
     * @brief Check if sensor is initialized
     * @return true if initialized, false otherwise
     */
    bool isInitialized() const { return initialized; }

private:
    max31865_t dev;           ///< MAX31865 device descriptor
    bool initialized = false; ///< Initialization status
    
    static const char* TAG;   ///< Log tag
};

#endif // PT100_SENSOR_HPP