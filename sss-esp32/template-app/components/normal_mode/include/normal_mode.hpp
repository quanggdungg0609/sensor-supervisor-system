#ifndef NORMAL_MODE_HPP
#define NORMAL_MODE_HPP

#include "storage_manager.hpp"
#include "sensor_manager.hpp"

#include "mqtt_client.h" 

class NormalMode {
public:
    /**
     * @brief Constructor for NormalMode
     * @param config Application configuration structure
     */
    NormalMode(const app_config_t& config);
    
    /**
     * @brief Start normal mode operation
     */
    void start();
    
private:
    void connect_wifi();
    void connect_mqtt();
    
    static void wifi_event_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data);
    static void mqtt_event_handler(void* handler_args, esp_event_base_t base, int32_t event_id, void* event_data);
    
    /**
     * @brief Send power outage alert to MQTT broker
     */
    void sendPowerOutageAlert();
    
    /**
     * @brief Get power outage alert count from NVS storage
     * @return Number of times power outage alert has been sent
     */
    int getPowerOutageAlertCount();
    
    /**
     * @brief Increment power outage alert count in NVS storage
     */
    void incrementPowerOutageAlertCount();
    
    /**
     * @brief Reset power outage alert count in NVS storage
     */
    void resetPowerOutageAlertCount();
    
    app_config_t app_config;
    esp_mqtt_client_handle_t mqtt_client;
    SensorManager sensor_manager;
    bool is_power_outage_wakeup = false;
    static const int MAX_POWER_OUTAGE_ALERTS = 3;
};

#endif // NORMAL_MODE_HPP