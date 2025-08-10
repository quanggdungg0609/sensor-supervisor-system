#include "nvs_flash.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "driver/gpio.h"
#include "esp_sleep.h"

#include "storage_manager.hpp"
#include "configuration_mode.hpp"
#include "normal_mode.hpp"
#include "alert_mode.hpp"

static const char* TAG = "MAIN_APP";

// Boot button pin
#define BOOT_BUTTON_PIN GPIO_NUM_0

extern "C" void app_main(void) {
    // Initialize NVS
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
        ESP_ERROR_CHECK(nvs_flash_erase());
        ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);
    
    ESP_LOGI(TAG, "ESP32 Application Started");
    
    // Configure boot button
    gpio_config_t io_conf = {};
    io_conf.intr_type = GPIO_INTR_DISABLE;
    io_conf.mode = GPIO_MODE_INPUT;
    io_conf.pin_bit_mask = (1ULL << BOOT_BUTTON_PIN);
    io_conf.pull_down_en = GPIO_PULLDOWN_DISABLE;
    io_conf.pull_up_en = GPIO_PULLUP_ENABLE;
    gpio_config(&io_conf);
    
    // Check if boot button is pressed
    if (gpio_get_level(BOOT_BUTTON_PIN) == 0) {
        ESP_LOGI(TAG, "Boot button pressed. Starting Configuration Mode.");
        static ConfigurationMode config_mode;
        config_mode.start();
    } else {
        // Check wake-up reason
        uint32_t wakeup_causes = esp_sleep_get_wakeup_causes();
        
        // Load the saved configuration from NVS
        app_config_t cfg = {};
        StorageManager::getInstance().load_config(cfg);
        
        // Initialize power detector to check current status
        PowerOutageDetector power_detector;
        power_detector.init(GPIO_NUM_4);
        
        // Check if max alerts flag is set
        AlertMode temp_alert(cfg);
        if (temp_alert.isMaxAlertsReached()) {
            // Max alerts reached, force normal mode regardless of power status
            ESP_LOGI(TAG, "Max alerts reached, forcing Normal Mode.");
            temp_alert.setMaxAlertsReached(false); // Reset flag
            static NormalMode normal_mode(cfg);
            normal_mode.start();
        } else if (wakeup_causes & ESP_SLEEP_WAKEUP_EXT0) {
            // Power outage detected, start Alert Mode
            ESP_LOGW(TAG, "Power outage detected. Starting Alert Mode.");
            static AlertMode alert_mode(cfg);
            alert_mode.start();
        } else if (power_detector.isPowerOutage()) {
            // Power is still out even on normal wake-up
            ESP_LOGW(TAG, "Power still out on normal wake-up. Starting Alert Mode.");
            static AlertMode alert_mode(cfg);
            alert_mode.start();
        } else {
            // Normal wake-up with power available, start Normal Mode
            ESP_LOGI(TAG, "Normal wake-up. Starting Normal Mode.");
            static NormalMode normal_mode(cfg);
            normal_mode.start();
        }
    }
    
    ESP_LOGI(TAG, "App main finished.");
}
