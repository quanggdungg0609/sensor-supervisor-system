#include "storage_manager.hpp"
#include "nvs_flash.h"
#include "nvs.h"
#include "esp_log.h"
#include <string.h>

#define STORAGE_NAMESPACE "esp_config"
static const char* TAG = "STORAGE";

esp_err_t StorageManager::save_config(const app_config_t& config) {
    nvs_handle_t my_handle;
    esp_err_t err = nvs_open(STORAGE_NAMESPACE, NVS_READWRITE, &my_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error (%s) opening NVS handle!", esp_err_to_name(err));
        return err;
    }

    // Write values to NVS
    nvs_set_str(my_handle, "device_name", config.device_name);
    nvs_set_str(my_handle, "ssid", config.ssid);
    nvs_set_str(my_handle, "password", config.password);
    nvs_set_str(my_handle, "mqtt_server", config.mqtt_server);
    nvs_set_u16(my_handle, "mqtt_port", config.mqtt_port);
    nvs_set_str(my_handle, "mqtt_user", config.mqtt_user);
    nvs_set_str(my_handle, "mqtt_pass", config.mqtt_pass);
    nvs_set_str(my_handle, "mqtt_client", config.mqtt_client_id);
    
    // Commit written value.
    err = nvs_commit(my_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to commit NVS changes! (%s)", esp_err_to_name(err));
    } else {
        ESP_LOGI(TAG, "Configuration saved successfully.");
    }

    // Close NVS
    nvs_close(my_handle);
    return err;
}

esp_err_t StorageManager::load_config(app_config_t& config) {
    nvs_handle_t my_handle;
    esp_err_t err = nvs_open(STORAGE_NAMESPACE, NVS_READONLY, &my_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error (%s) opening NVS handle for reading!", esp_err_to_name(err));
        return err;
    }

    size_t required_size;

    // --- BỔ SUNG ĐẦY ĐỦ CÁC TRƯỜNG ---

    if (nvs_get_str(my_handle, "device_name", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "device_name", config.device_name, &required_size);
    }

    if (nvs_get_str(my_handle, "ssid", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "ssid", config.ssid, &required_size);
    }

    if (nvs_get_str(my_handle, "password", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "password", config.password, &required_size);
    }

    if (nvs_get_str(my_handle, "mqtt_server", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "mqtt_server", config.mqtt_server, &required_size);
    }
    
    nvs_get_u16(my_handle, "mqtt_port", &config.mqtt_port);

    if (nvs_get_str(my_handle, "mqtt_user", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "mqtt_user", config.mqtt_user, &required_size);
    }
    
    if (nvs_get_str(my_handle, "mqtt_pass", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "mqtt_pass", config.mqtt_pass, &required_size);
    }

    if (nvs_get_str(my_handle, "mqtt_client", NULL, &required_size) == ESP_OK) {
        nvs_get_str(my_handle, "mqtt_client", config.mqtt_client_id, &required_size);
    }


    nvs_close(my_handle);
    ESP_LOGI(TAG, "Configuration loaded.");
    return ESP_OK;
}