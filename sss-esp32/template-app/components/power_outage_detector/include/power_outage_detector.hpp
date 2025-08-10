#ifndef POWER_OUTAGE_DETECTOR_HPP
#define POWER_OUTAGE_DETECTOR_HPP

#include "esp_err.h"
#include "driver/gpio.h"

/**
 * @brief Power Outage Detector class for monitoring power status via GPIO
 */
class PowerOutageDetector {
public:
    /**
     * @brief Constructor
     */
    PowerOutageDetector();
    
    /**
     * @brief Destructor
     */
    ~PowerOutageDetector();
    
    /**
     * @brief Initialize the power outage detector
     * @param gpio_pin GPIO pin number to monitor (default: GPIO_NUM_4)
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t init(gpio_num_t gpio_pin = GPIO_NUM_4);
    
    /**
     * @brief Get current power status
     * @return 1 if power is available, 0 if power outage detected
     */
    int getPowerStatus();
    
    /**
     * @brief Check if power outage occurred (for wake-up detection)
     * @return true if power outage detected, false otherwise
     */
    bool isPowerOutage();
    
    /**
     * @brief Configure GPIO for deep sleep wake-up on power outage
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t configureWakeUp();
    
    /**
     * @brief Disable GPIO wake-up configuration
     * @return ESP_OK on success, error code otherwise
     */
    esp_err_t disableWakeUp();
    
private:
    static const char* TAG;           ///< Logging tag
    gpio_num_t m_gpio_pin;           ///< GPIO pin for power monitoring
    bool m_initialized;              ///< Initialization status
};

#endif // POWER_OUTAGE_DETECTOR_HPP