#include "nvs_flash.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "driver/gpio.h"

#include "storage_manager.hpp"
#include "configuration_mode.hpp"
#include "normal_mode.hpp"

static const char* TAG = "MAIN_APP";

// The BOOT button on most ESP32 boards is connected to GPIO 0
#define BOOT_BUTTON_PIN GPIO_NUM_0

extern "C" void app_main(void) {
    // Initialize NVS (Non-Volatile Storage)
    esp_err_t ret = nvs_flash_init();
    if (ret == ESP_ERR_NVS_NO_FREE_PAGES || ret == ESP_ERR_NVS_NEW_VERSION_FOUND) {
      ESP_ERROR_CHECK(nvs_flash_erase());
      ret = nvs_flash_init();
    }
    ESP_ERROR_CHECK(ret);

    // Configure GPIO pin for the BOOT button
    gpio_config_t io_conf = {};
    io_conf.intr_type = GPIO_INTR_DISABLE;
    io_conf.mode = GPIO_MODE_INPUT;
    io_conf.pin_bit_mask = (1ULL << BOOT_BUTTON_PIN);
    io_conf.pull_up_en = GPIO_PULLUP_ENABLE; 
    gpio_config(&io_conf);

    ESP_LOGI(TAG, "Checking boot button state...");
    // Wait a moment for the pin state to stabilize
    vTaskDelay(pdMS_TO_TICKS(100));
    // If the button is pressed (LOW level), enter configuration mode
    if (gpio_get_level(BOOT_BUTTON_PIN) == 0) {
        ESP_LOGI(TAG, "Boot button pressed. Starting Configuration Mode.");
        ConfigurationMode config_mode;
        config_mode.start();
    } else {
        ESP_LOGI(TAG, "Boot button not pressed. Starting Normal Mode.");
        
        // Load the saved configuration from NVS
        app_config_t cfg = {};
        StorageManager::getInstance().load_config(cfg);
        
        // Pass the loaded configuration to NormalMode and start it
        static NormalMode normal_mode(cfg);
        normal_mode.start();
    }
    
    ESP_LOGI(TAG, "App main finished.");
}
