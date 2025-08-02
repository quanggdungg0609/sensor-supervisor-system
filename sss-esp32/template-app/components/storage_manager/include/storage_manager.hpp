#ifndef STORAGE_MANAGER_HPP
#define STORAGE_MANAGER_HPP

#include "esp_err.h"
#include <cstdint>

// A struct to hold all configuration data.
typedef struct {
    char device_name[32];
    char ssid[32];
    char password[64];
    char mqtt_server[40];
    uint16_t mqtt_port;
    char mqtt_user[32];
    char mqtt_pass[32];
    char mqtt_client_id[64];
} app_config_t;

class StorageManager {
public:
    // Get the single instance of the class (Singleton Pattern).
    static StorageManager& getInstance() {
        static StorageManager instance; // Guaranteed to be destroyed. Instantiated on first use.
        return instance;
    }

    // Delete copy constructor and assignment operator to prevent multiple instances.
    StorageManager(const StorageManager&) = delete;
    void operator=(const StorageManager&) = delete;

    // Save configuration to NVS.
    esp_err_t save_config(const app_config_t& config);

    // Load configuration from NVS.
    esp_err_t load_config(app_config_t& config);

private:
    // Private constructor to prevent external instantiation.
    StorageManager() = default; 
};

#endif // STORAGE_MANAGER_HPP