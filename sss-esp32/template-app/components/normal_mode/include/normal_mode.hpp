#ifndef NORMAL_MODE_HPP
#define NORMAL_MODE_HPP

#include "storage_manager.hpp"
#include "sensor_manager.hpp"

#include "mqtt_client.h" 

class NormalMode {
public:
    // Constructor receives the loaded configuration.
    explicit NormalMode(const app_config_t& config);
    void start();

private:
    void connect_wifi();
    void connect_mqtt();
    
    static void wifi_event_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data);
    static void mqtt_event_handler(void* handler_args, esp_event_base_t base, int32_t event_id, void* event_data);

    app_config_t app_config;
    esp_mqtt_client_handle_t mqtt_client;
    SensorManager sensor_manager;
};

#endif // NORMAL_MODE_HPP