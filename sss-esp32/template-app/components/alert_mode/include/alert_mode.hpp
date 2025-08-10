#ifndef ALERT_MODE_HPP
#define ALERT_MODE_HPP

#include "storage_manager.hpp"
#include "sensor_manager.hpp"
#include "mqtt_client.h"
#include "esp_event.h"
#include "esp_timer.h"

class AlertMode {
public:
    /**
     * @brief Constructor
     * @param config Application configuration
     */
    AlertMode(const app_config_t& config);
    
    /**
     * @brief Start alert mode
     */
    void start();
    
    /**
     * @brief Check if maximum alerts have been reached
     * @return true if max alerts reached, false otherwise
     */
    bool isMaxAlertsReached();
    
    /**
     * @brief Set maximum alerts reached flag
     * @param reached true to set flag, false to clear
     */
    void setMaxAlertsReached(bool reached);

private:
    app_config_t app_config;
    esp_mqtt_client_handle_t mqtt_client;
    SensorManager sensor_manager;
    esp_timer_handle_t alert_timer;
    esp_timer_handle_t telemetry_timer;
    bool power_restored;
    static const int MAX_POWER_OUTAGE_ALERTS = 3;
    static const int ALERT_INTERVAL_SECONDS = 30;
    static const int TELEMETRY_INTERVAL_SECONDS = 300; // 5 minutes like normal mode
    
    /**
     * @brief Connect to WiFi network
     */
    void connect_wifi();
    
    /**
     * @brief Connect to MQTT broker
     */
    void connect_mqtt();
    
    /**
     * @brief Send power outage alert to MQTT broker
     */
    void sendPowerOutageAlert();
    
    /**
     * @brief Send telemetry data to MQTT broker
     */
    void sendTelemetryData();
    
    /**
     * @brief Start periodic alert timer
     */
    void startAlertTimer();
    
    /**
     * @brief Start periodic telemetry timer
     */
    void startTelemetryTimer();
    
    /**
     * @brief Stop all timers
     */
    void stopTimers();
    
    /**
     * @brief Check power status periodically
     */
    void checkPowerStatus();
    
    /**
     * @brief Main alert mode loop
     */
    void alertModeLoop();
    
    /**
     * @brief Get current power outage alert count from NVS
     * @return Number of alerts sent
     */
    int getPowerOutageAlertCount();
    
    /**
     * @brief Increment power outage alert count in NVS
     */
    void incrementPowerOutageAlertCount();
    
    /**
     * @brief Reset power outage alert count in NVS
     */
    void resetPowerOutageAlertCount();
    
    /**
     * @brief WiFi event handler
     */
    static void wifi_event_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data);
    
    /**
     * @brief MQTT event handler
     */
    static void mqtt_event_handler(void* handler_args, esp_event_base_t base, int32_t event_id, void* event_data);
    
    /**
     * @brief Alert timer callback
     */
    static void alert_timer_callback(void* arg);
    
    /**
     * @brief Telemetry timer callback
     */
    static void telemetry_timer_callback(void* arg);
};

#endif // ALERT_MODE_HPP