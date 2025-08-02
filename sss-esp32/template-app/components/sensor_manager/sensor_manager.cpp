#include "sensor_manager.hpp"
#include "ntp_client.hpp"
#include "ArduinoJson.h"
#include "esp_log.h"
#include "driver/spi_master.h"
#include "driver/gpio.h" 
#include <cmath>

#define SENSOR_SPI_HOST    SPI2_HOST 

#define PIN_NUM_MISO    GPIO_NUM_19
#define PIN_NUM_MOSI    GPIO_NUM_23
#define PIN_NUM_CLK     GPIO_NUM_18
#define PIN_NUM_CS      GPIO_NUM_5

#define R_REF           430.0
#define RTD_NOMINAL     100.0
#define RTD_STANDARD    MAX31865_ITS90

static const char* TAG = "SENSOR_MANAGER";


bool SensorManager::init() {
    spi_bus_config_t buscfg = {
        .mosi_io_num = PIN_NUM_MOSI,
        .miso_io_num = PIN_NUM_MISO,
        .sclk_io_num = PIN_NUM_CLK,
        .quadwp_io_num = -1,
        .quadhd_io_num = -1,
    };
    ESP_ERROR_CHECK(spi_bus_initialize(SENSOR_SPI_HOST, &buscfg, SPI_DMA_CH_AUTO));

    dev.standard = RTD_STANDARD;
    dev.r_ref = R_REF;
    dev.rtd_nominal = RTD_NOMINAL;
    ESP_ERROR_CHECK(max31865_init_desc(&dev, SENSOR_SPI_HOST, 1000000, PIN_NUM_CS));

    max31865_config_t config;
    config.mode = MAX31865_MODE_SINGLE;
    config.v_bias = true;
    config.connection = MAX31865_2WIRE;
    config.filter = MAX31865_FILTER_50HZ;
    ESP_ERROR_CHECK(max31865_set_config(&dev, &config));

    ESP_LOGI(TAG, "MAX31865 sensor initialized successfully.");
    return true;
}

std::string SensorManager::getAggregatedDataJson() {
    JsonDocument doc;
    doc["timestamp"] = NTPClient::getInstance().getFormattedTimestamp();
    
    JsonObject data = doc["data"].to<JsonObject>();
    data["temp"] = getTemperature();
    
    std::string output;
    serializeJson(doc, output);
    return output;
}

float SensorManager::getTemperature() {
    float temp;
    esp_err_t err = max31865_measure(&dev, &temp);

    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to measure temperature: %s", esp_err_to_name(err));
        
        uint8_t fault;
        if (max31865_get_fault_status(&dev, &fault) == ESP_OK) {
            ESP_LOGE(TAG, "Detailed fault status: 0x%02x", fault);
        }

        return NAN;
    }

    ESP_LOGI(TAG, "Temperature read: %.2f *C", temp);
    return temp;
}

float SensorManager::getHumidity() {
    return NAN;
}