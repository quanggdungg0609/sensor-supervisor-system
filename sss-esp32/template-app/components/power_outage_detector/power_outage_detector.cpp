#include "power_outage_detector.hpp"
#include "esp_log.h"
#include "esp_sleep.h"
#include "driver/rtc_io.h"

const char* PowerOutageDetector::TAG = "POWER_OUTAGE";

/**
 * @brief Constructor
 */
PowerOutageDetector::PowerOutageDetector() 
    : m_gpio_pin(GPIO_NUM_4), m_initialized(false) {
}

/**
 * @brief Destructor
 */
PowerOutageDetector::~PowerOutageDetector() {
    if (m_initialized) {
        gpio_reset_pin(m_gpio_pin);
    }
}

/**
 * @brief Initialize the power outage detector
 * @param gpio_pin GPIO pin number to monitor
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t PowerOutageDetector::init(gpio_num_t gpio_pin) {
    m_gpio_pin = gpio_pin;
    
    // Configure GPIO as input with pull-up
    gpio_config_t io_conf = {};
    io_conf.intr_type = GPIO_INTR_DISABLE;
    io_conf.mode = GPIO_MODE_INPUT;
    io_conf.pin_bit_mask = (1ULL << m_gpio_pin);
    io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    io_conf.pull_up_en = GPIO_PULLUP_ENABLE;
    
    esp_err_t ret = gpio_config(&io_conf);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to configure GPIO%d: %s", m_gpio_pin, esp_err_to_name(ret));
        return ret;
    }
    
    m_initialized = true;
    ESP_LOGI(TAG, "Power outage detector initialized on GPIO%d", m_gpio_pin);
    
    return ESP_OK;
}

/**
 * @brief Get current power status
 * @return 1 if power is available, 0 if power outage detected
 */
int PowerOutageDetector::getPowerStatus() {
    if (!m_initialized) {
        ESP_LOGE(TAG, "Power outage detector not initialized");
        return 0;
    }
    
    // Read GPIO level
    // When strap is connected to GND: GPIO reads 0 (power available)
    // When strap is disconnected: GPIO reads 1 (power outage)
    int gpio_level = gpio_get_level(m_gpio_pin);
    
    // Invert logic: 0 = power outage, 1 = power available
    return (gpio_level == 0) ? 1 : 0;
}

/**
 * @brief Check if power outage occurred (for wake-up detection)
 * @return true if power outage detected, false otherwise
 */
bool PowerOutageDetector::isPowerOutage() {
    return (getPowerStatus() == 0);
}

/**
 * @brief Configure GPIO for deep sleep wake-up on power outage
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t PowerOutageDetector::configureWakeUp() {
    if (!m_initialized) {
        ESP_LOGE(TAG, "Power outage detector not initialized");
        return ESP_ERR_INVALID_STATE;
    }
    
    // Configure RTC GPIO for wake-up
    if (!rtc_gpio_is_valid_gpio(m_gpio_pin)) {
        ESP_LOGE(TAG, "GPIO%d is not RTC GPIO, cannot use for wake-up", m_gpio_pin);
        return ESP_ERR_INVALID_ARG;
    }
    
    // Configure RTC GPIO
    esp_err_t ret = rtc_gpio_init(m_gpio_pin);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to init RTC GPIO%d: %s", m_gpio_pin, esp_err_to_name(ret));
        return ret;
    }
    
    ret = rtc_gpio_set_direction(m_gpio_pin, RTC_GPIO_MODE_INPUT_ONLY);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to set RTC GPIO%d direction: %s", m_gpio_pin, esp_err_to_name(ret));
        return ret;
    }
    
    ret = rtc_gpio_pullup_en(m_gpio_pin);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to enable RTC GPIO%d pullup: %s", m_gpio_pin, esp_err_to_name(ret));
        return ret;
    }
    
    ret = rtc_gpio_pulldown_dis(m_gpio_pin);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to disable RTC GPIO%d pulldown: %s", m_gpio_pin, esp_err_to_name(ret));
        return ret;
    }
    
    // Configure wake-up on rising edge (when strap disconnects)
    ret = esp_sleep_enable_ext0_wakeup(m_gpio_pin, 1);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to enable ext0 wakeup: %s", esp_err_to_name(ret));
        return ret;
    }
    
    ESP_LOGI(TAG, "Configured wake-up on power outage for GPIO%d", m_gpio_pin);
    return ESP_OK;
}

/**
 * @brief Disable GPIO wake-up configuration
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t PowerOutageDetector::disableWakeUp() {
    if (!m_initialized) {
        ESP_LOGE(TAG, "Power outage detector not initialized");
        return ESP_ERR_INVALID_STATE;
    }
    
    // Disable ext0 wake-up
    esp_sleep_disable_wakeup_source(ESP_SLEEP_WAKEUP_EXT0);
    
    ESP_LOGI(TAG, "Disabled GPIO wake-up for power outage detection");
    return ESP_OK;
}