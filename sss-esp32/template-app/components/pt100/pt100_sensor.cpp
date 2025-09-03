#include "pt100_sensor.hpp"
#include "esp_log.h"
#include "driver/spi_master.h"
#include "driver/gpio.h"
#include <cmath>

// SPI Configuration for XIAO ESP32C6
#define SENSOR_SPI_HOST    SPI2_HOST 
#define PIN_NUM_MISO       GPIO_NUM_20  // D9 on XIAO ESP32C6
#define PIN_NUM_MOSI       GPIO_NUM_18  // D10 on XIAO ESP32C6
#define PIN_NUM_CLK        GPIO_NUM_19  // D8 on XIAO ESP32C6
#define PIN_NUM_CS         GPIO_NUM_17  // D7 on XIAO ESP32C6

// PT100 Configuration
#define R_REF              430.0
#define RTD_NOMINAL        100.0
#define RTD_STANDARD       MAX31865_ITS90

const char* PT100Sensor::TAG = "PT100_SENSOR";

/**
 * @brief Initialize the PT100 sensor with MAX31865 amplifier
 * @return true if initialization successful, false otherwise
 */
bool PT100Sensor::init() {
    if (initialized) {
        ESP_LOGW(TAG, "PT100 sensor already initialized");
        return true;
    }
    
    // Configure SPI bus for XIAO ESP32C6
    spi_bus_config_t buscfg = {
        .mosi_io_num = PIN_NUM_MOSI,
        .miso_io_num = PIN_NUM_MISO,
        .sclk_io_num = PIN_NUM_CLK,
        .quadwp_io_num = -1,
        .quadhd_io_num = -1,
    };
    
    esp_err_t ret = spi_bus_initialize(SENSOR_SPI_HOST, &buscfg, SPI_DMA_CH_AUTO);
    if (ret != ESP_OK && ret != ESP_ERR_INVALID_STATE) {
        ESP_LOGE(TAG, "Failed to initialize SPI bus: %s", esp_err_to_name(ret));
        return false;
    }
    
    // Initialize MAX31865 device
    dev.standard = RTD_STANDARD;
    dev.r_ref = R_REF;
    dev.rtd_nominal = RTD_NOMINAL;
    
    ret = max31865_init_desc(&dev, SENSOR_SPI_HOST, 1000000, PIN_NUM_CS);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to initialize MAX31865 descriptor: %s", esp_err_to_name(ret));
        return false;
    }
    
    // Configure MAX31865 for PT100 sensor
    max31865_config_t config;
    config.mode = MAX31865_MODE_SINGLE;
    config.v_bias = true;
    config.connection = MAX31865_2WIRE;  // Use 2-wire connection for PT100
    config.filter = MAX31865_FILTER_50HZ;
    
    ret = max31865_set_config(&dev, &config);
    if (ret != ESP_OK) {
        ESP_LOGE(TAG, "Failed to configure MAX31865: %s", esp_err_to_name(ret));
        return false;
    }
    
    initialized = true;
    ESP_LOGI(TAG, "PT100 sensor with MAX31865 initialized successfully on XIAO ESP32C6");
    return true;
}

/**
 * @brief Read temperature from PT100 sensor
 * @return Temperature in Celsius, NAN if error occurred
 */
float PT100Sensor::readTemperature() {
    if (!initialized) {
        ESP_LOGE(TAG, "PT100 sensor not initialized");
        return NAN;
    }
    
    float temp;
    esp_err_t err = max31865_measure(&dev, &temp);
    
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to measure temperature: %s", esp_err_to_name(err));
        
        // Get detailed fault information for debugging
        uint8_t fault;
        if (max31865_get_fault_status(&dev, &fault) == ESP_OK) {
            ESP_LOGE(TAG, "Detailed fault status: 0x%02x", fault);
        }
        
        return NAN;
    }
    
    ESP_LOGI(TAG, "PT100 temperature read: %.2f °C", temp);
    return temp;
}