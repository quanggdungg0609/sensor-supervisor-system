#include "sht30_sensor.hpp"
#include "esp_log.h"
#include "driver/i2c_master.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include <cmath>

const char* SHT30Sensor::TAG = "SHT30";

/**
 * @brief Default constructor for SHT30Sensor
 */
SHT30Sensor::SHT30Sensor() : m_bus_handle(nullptr), m_dev_handle(nullptr), m_initialized(false) {
    ESP_LOGI(TAG, "SHT30Sensor constructor called");
}

/**
 * @brief Destructor for SHT30Sensor
 */
SHT30Sensor::~SHT30Sensor() {
    if (m_dev_handle) {
        i2c_master_bus_rm_device(m_dev_handle);
    }
    if (m_bus_handle) {
        i2c_del_master_bus(m_bus_handle);
    }
    ESP_LOGI(TAG, "SHT30Sensor destructor called");
}

/**
 * @brief Initialize the SHT30 sensor with specified GPIO pins
 * @param sda_pin GPIO pin for SDA (default: 21)
 * @param scl_pin GPIO pin for SCL (default: 22)
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t SHT30Sensor::init(int sda_pin, int scl_pin) {
    ESP_LOGI(TAG, "Initializing SHT30 sensor with SDA=%d, SCL=%d", sda_pin, scl_pin);
    
    // Configure I2C master bus
    i2c_master_bus_config_t i2c_mst_config = {
        .i2c_port = I2C_NUM_0,
        .sda_io_num = static_cast<gpio_num_t>(sda_pin),
        .scl_io_num = static_cast<gpio_num_t>(scl_pin),
        .clk_source = I2C_CLK_SRC_DEFAULT,
        .glitch_ignore_cnt = 7,
        .intr_priority = 0,
        .trans_queue_depth = 0,
        .flags = {
            .enable_internal_pullup = true,
        },
    };
    
    esp_err_t ret = i2c_new_master_bus(&i2c_mst_config, &m_bus_handle);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to create I2C master bus: %s", esp_err_to_name(ret));
        return ret;
    }
    
    // Configure I2C device
    i2c_device_config_t dev_cfg = {
        .dev_addr_length = I2C_ADDR_BIT_LEN_7,
        .device_address = SHT30_ADDR,
        .scl_speed_hz = I2C_FREQ_HZ,
        .scl_wait_us = 0,
        .flags = {
            .disable_ack_check = false,
        },
    };
    
    ret = i2c_master_bus_add_device(m_bus_handle, &dev_cfg, &m_dev_handle);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to add I2C device: %s", esp_err_to_name(ret));
        i2c_del_master_bus(m_bus_handle);
        m_bus_handle = nullptr;
        return ret;
    }
    
    // Test communication with sensor
    for (int attempt = 1; attempt <= 3; attempt++) {
        ESP_LOGI(TAG, "Testing SHT30 communication, attempt %d/3", attempt);
        
        float temp, hum;
        ret = readTempHumidity(&temp, &hum);
        if (ret == ESP_OK) {
            ESP_LOGI(TAG, "SHT30 sensor initialized successfully");
            ESP_LOGI(TAG, "Initial reading - Temperature: %.2f°C, Humidity: %.2f%%", temp, hum);
            m_initialized = true;
            return ESP_OK;
        }
        
        ESP_LOGW(TAG, "Communication test failed: %s", esp_err_to_name(ret));
        if (attempt < 3) {
            vTaskDelay(pdMS_TO_TICKS(1000));
        }
    }
    
    ESP_LOGE(TAG, "Failed to communicate with SHT30 sensor after 3 attempts: %s", esp_err_to_name(ret));
    
    // Cleanup on failure
    i2c_master_bus_rm_device(m_dev_handle);
    i2c_del_master_bus(m_bus_handle);
    m_dev_handle = nullptr;
    m_bus_handle = nullptr;
    
    return ret;
}

/**
 * @brief Read temperature and humidity from sensor
 * @param temperature Pointer to store temperature value (°C)
 * @param humidity Pointer to store humidity value (%RH)
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t SHT30Sensor::readTempHumidity(float* temperature, float* humidity) {
    if (!m_dev_handle) {
        ESP_LOGE(TAG, "Device handle not initialized");
        return ESP_ERR_INVALID_STATE;
    }
    
    if (!temperature || !humidity) {
        ESP_LOGE(TAG, "Invalid parameters");
        return ESP_ERR_INVALID_ARG;
    }
    
    // Send measurement command
    esp_err_t ret = sendMeasurementCommand();
    if (ret != ESP_OK) {
        return ret;
    }
    
    // Wait for measurement to complete
    vTaskDelay(pdMS_TO_TICKS(20));
    
    // Read raw data
    uint8_t data[6];
    ret = readRawData(data);
    if (ret != ESP_OK) {
        return ret;
    }
    
    // Convert raw data to temperature and humidity
    uint16_t temp_raw = (data[0] << 8) | data[1];
    uint16_t hum_raw = (data[3] << 8) | data[4];
    
    *temperature = -45.0f + 175.0f * temp_raw / 65535.0f;
    *humidity = 100.0f * hum_raw / 65535.0f;
    
    ESP_LOGD(TAG, "Temperature: %.2f°C, Humidity: %.2f%%", *temperature, *humidity);
    
    return ESP_OK;
}

/**
 * @brief Get temperature reading
 * @return Temperature in Celsius, NaN if error
 */
float SHT30Sensor::getTemperature() {
    float temp, hum;
    if (readTempHumidity(&temp, &hum) == ESP_OK) {
        return temp;
    }
    return NAN;
}

/**
 * @brief Get humidity reading
 * @return Humidity in %RH, NaN if error
 */
float SHT30Sensor::getHumidity() {
    float temp, hum;
    if (readTempHumidity(&temp, &hum) == ESP_OK) {
        return hum;
    }
    return NAN;
}

/**
 * @brief Check if sensor is available and initialized
 * @return true if sensor is available, false otherwise
 */
bool SHT30Sensor::isAvailable() {
    return m_initialized && m_dev_handle != nullptr;
}

/**
 * @brief Send measurement command to sensor
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t SHT30Sensor::sendMeasurementCommand() {
    // SHT30 single shot measurement command (high repeatability)
    uint8_t cmd[2] = {0x2C, 0x06};
    
    esp_err_t ret = i2c_master_transmit(m_dev_handle, cmd, sizeof(cmd), pdMS_TO_TICKS(1000));
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to send measurement command: %s", esp_err_to_name(ret));
    }
    
    return ret;
}

/**
 * @brief Read raw data from sensor
 * @param data Buffer to store 6 bytes of data
 * @return ESP_OK on success, error code otherwise
 */
esp_err_t SHT30Sensor::readRawData(uint8_t* data) {
    esp_err_t ret = i2c_master_receive(m_dev_handle, data, 6, pdMS_TO_TICKS(1000));
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to read raw data: %s", esp_err_to_name(ret));
    }
    
    return ret;
}