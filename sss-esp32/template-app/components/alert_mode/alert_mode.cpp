#include "alert_mode.hpp"
#include "sensor_manager.hpp"
#include "ntp_client.hpp"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "freertos/event_groups.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"
#include "esp_sleep.h"
#include "esp_timer.h"
#include "mqtt_client.h"
#include <string>
#include "nvs_flash.h"
#include "nvs.h"

static const char* TAG = "ALERT_MODE";
#define WIFI_CONNECTED_BIT BIT0
#define WIFI_FAIL_BIT      BIT1
static EventGroupHandle_t s_wifi_event_group;

AlertMode::AlertMode(const app_config_t& config) : 
    app_config(config), 
    mqtt_client(nullptr), 
    alert_timer(nullptr),
    telemetry_timer(nullptr),
    power_restored(false) {}

/**
 * @brief Start alert mode to handle power outage detection and alerts
 */
void AlertMode::start() {
    ESP_LOGI(TAG, "Starting Alert Mode...");
    ESP_LOGW(TAG, "=== POWER OUTAGE DETECTED ===");
    
    // Initialize sensor manager first
    if (!sensor_manager.init()) {
        ESP_LOGE(TAG, "Failed to initialize SensorManager! Restarting...");
        vTaskDelay(pdMS_TO_TICKS(2000));
        esp_restart();
    }
    
    // Initialize WiFi and MQTT
    connect_wifi();
    connect_mqtt();
    
    // Start the main alert mode loop
    alertModeLoop();
}

/**
 * @brief Main alert mode loop - stays active until power is restored
 */
void AlertMode::alertModeLoop() {
    ESP_LOGI(TAG, "Entering alert mode loop...");
    
    // Send immediate power outage alert
    ESP_LOGI(TAG, "Sending immediate power outage alert...");
    sendPowerOutageAlert();
    
    // Start timers for periodic alerts and telemetry
    startAlertTimer();
    startTelemetryTimer();
    
    // Main loop - check power status every 5 seconds
    while (!power_restored) {
        checkPowerStatus();
        vTaskDelay(pdMS_TO_TICKS(5000)); // Check every 5 seconds
    }
    
    // Power restored - cleanup and transition to normal mode
    ESP_LOGI(TAG, "Power restored! Cleaning up alert mode...");
    stopTimers();
    resetPowerOutageAlertCount();
    setMaxAlertsReached(false);
    
    // Configure GPIO wake-up for normal operation and restart
    sensor_manager.configurePowerOutageWakeUp();
    esp_restart();
}

/**
 * @brief Check power status and update power_restored flag
 */
void AlertMode::checkPowerStatus() {
    // Check current power status multiple times to ensure stability
    int stable_readings = 0;
    int power_status = 0;
    
    for (int i = 0; i < 5; i++) {
        int current_reading = sensor_manager.getPowerStatus();
        if (current_reading == power_status) {
            stable_readings++;
        } else {
            power_status = current_reading;
            stable_readings = 1;
        }
        vTaskDelay(pdMS_TO_TICKS(200));
    }
    
    // Only consider power restored if we have stable readings
    if (power_status == 1 && stable_readings >= 3) {
        ESP_LOGI(TAG, "Power status: RESTORED (stable readings: %d)", stable_readings);
        power_restored = true;
    } else {
        ESP_LOGD(TAG, "Power status: OUTAGE (status: %d, stable: %d)", power_status, stable_readings);
    }
}

/**
 * @brief Start periodic alert timer
 */
void AlertMode::startAlertTimer() {
    if (alert_timer != nullptr) {
        esp_timer_stop(alert_timer);
        esp_timer_delete(alert_timer);
    }
    
    esp_timer_create_args_t alert_timer_args = {
        .callback = &AlertMode::alert_timer_callback,
        .arg = this,
        .dispatch_method = ESP_TIMER_TASK,
        .name = "alert_timer",
        .skip_unhandled_events = false
    };
    
    ESP_ERROR_CHECK(esp_timer_create(&alert_timer_args, &alert_timer));
    ESP_ERROR_CHECK(esp_timer_start_periodic(alert_timer, ALERT_INTERVAL_SECONDS * 1000000ULL));
    
    ESP_LOGI(TAG, "Alert timer started - will send alerts every %d seconds", ALERT_INTERVAL_SECONDS);
}

/**
 * @brief Start periodic telemetry timer
 */
void AlertMode::startTelemetryTimer() {
    if (telemetry_timer != nullptr) {
        esp_timer_stop(telemetry_timer);
        esp_timer_delete(telemetry_timer);
    }
    
    esp_timer_create_args_t telemetry_timer_args = {
        .callback = &AlertMode::telemetry_timer_callback,
        .arg = this,
        .dispatch_method = ESP_TIMER_TASK,
        .name = "telemetry_timer",
        .skip_unhandled_events = false
    };
    
    ESP_ERROR_CHECK(esp_timer_create(&telemetry_timer_args, &telemetry_timer));
    ESP_ERROR_CHECK(esp_timer_start_periodic(telemetry_timer, TELEMETRY_INTERVAL_SECONDS * 1000000ULL));
    
    ESP_LOGI(TAG, "Telemetry timer started - will send telemetry every %d seconds", TELEMETRY_INTERVAL_SECONDS);
}

/**
 * @brief Stop all timers
 */
void AlertMode::stopTimers() {
    if (alert_timer != nullptr) {
        esp_timer_stop(alert_timer);
        esp_timer_delete(alert_timer);
        alert_timer = nullptr;
    }
    
    if (telemetry_timer != nullptr) {
        esp_timer_stop(telemetry_timer);
        esp_timer_delete(telemetry_timer);
        telemetry_timer = nullptr;
    }
    
    ESP_LOGI(TAG, "All timers stopped");
}

/**
 * @brief Alert timer callback - sends power outage alerts
 */
void AlertMode::alert_timer_callback(void* arg) {
    AlertMode* alert_mode = static_cast<AlertMode*>(arg);
    
    int alert_count = alert_mode->getPowerOutageAlertCount();
    
    if (alert_count < MAX_POWER_OUTAGE_ALERTS) {
        ESP_LOGI(TAG, "Alert timer triggered - sending power outage alert (count: %d)", alert_count + 1);
        alert_mode->sendPowerOutageAlert();
    } else {
        ESP_LOGW(TAG, "Maximum alerts (%d) reached - stopping alert timer", MAX_POWER_OUTAGE_ALERTS);
        alert_mode->setMaxAlertsReached(true);
        if (alert_mode->alert_timer != nullptr) {
            esp_timer_stop(alert_mode->alert_timer);
            esp_timer_delete(alert_mode->alert_timer);
            alert_mode->alert_timer = nullptr;
        }
    }
}

/**
 * @brief Telemetry timer callback - sends sensor telemetry data
 */
void AlertMode::telemetry_timer_callback(void* arg) {
    AlertMode* alert_mode = static_cast<AlertMode*>(arg);
    
    ESP_LOGI(TAG, "Telemetry timer triggered - sending sensor data");
    alert_mode->sendTelemetryData();
}

/**
 * @brief Send power outage alert to MQTT broker
 */
void AlertMode::sendPowerOutageAlert() {
    if (!mqtt_client) {
        ESP_LOGE(TAG, "MQTT client not initialized");
        return;
    }
    
    int alert_count = getPowerOutageAlertCount();
    
    if (alert_count >= MAX_POWER_OUTAGE_ALERTS) {
        ESP_LOGW(TAG, "Maximum alerts reached, not sending more alerts");
        return;
    }
    
    // Read actual power status from sensor
    int power_status = sensor_manager.getPowerStatus();
    
    // Create power outage alert message with new format
    std::string timestamp = NTPClient::getInstance().getFormattedTimestamp();
    std::string alert_message = "{\"data\":{\"power_status\":" + std::to_string(power_status) + "},\"timestamp\":\"" + timestamp + "\"}";
    
    // Create topic string from mqtt_client_id
    std::string topic = "sensors/" + std::string(app_config.mqtt_client_id) + "/power_outage";
    
    // Log the message content before sending
    ESP_LOGI(TAG, "Sending power outage alert:");
    ESP_LOGI(TAG, "Topic: %s", topic.c_str());
    ESP_LOGI(TAG, "Message: %s", alert_message.c_str());
    ESP_LOGI(TAG, "Power Status: %d", power_status);
    
    // Publish alert message
    int msg_id = esp_mqtt_client_publish(mqtt_client, topic.c_str(), 
                                        alert_message.c_str(), alert_message.length(), 
                                        1, 0);
    
    if (msg_id != -1) {
        ESP_LOGW(TAG, "Power outage alert sent successfully (msg_id: %d, power_status: %d)", msg_id, power_status);
        incrementPowerOutageAlertCount();
    } else {
        ESP_LOGE(TAG, "Failed to send power outage alert");
    }
}

/**
 * @brief Send telemetry data to MQTT broker
 */
void AlertMode::sendTelemetryData() {
    if (!mqtt_client) {
        ESP_LOGE(TAG, "MQTT client not initialized");
        return;
    }
    
    // Read sensor data
    float temperature = sensor_manager.getTemperature();
    float humidity = sensor_manager.getHumidity();
    int power_status = sensor_manager.getPowerStatus();
    
    // Create telemetry message with new format
    std::string timestamp = NTPClient::getInstance().getFormattedTimestamp();
    std::string telemetry_data = "{\"data\":{\"temperature\":" + std::to_string(temperature) + 
                               ",\"humidity\":" + std::to_string(humidity) + 
                               ",\"power_status\":" + std::to_string(power_status) + 
                               "},\"timestamp\":\"" + timestamp + "\"}";
    
    // Create topic string
    std::string topic = "sensors/" + std::string(app_config.mqtt_client_id) + "/telemetry";
    
    // Log the message content before sending
    ESP_LOGI(TAG, "Sending telemetry data:");
    ESP_LOGI(TAG, "Topic: %s", topic.c_str());
    ESP_LOGI(TAG, "Message: %s", telemetry_data.c_str());
    ESP_LOGI(TAG, "Sensor readings - Temperature: %.2f°C, Humidity: %.2f%%, Power Status: %d", temperature, humidity, power_status);
    
    // Publish telemetry message
    int msg_id = esp_mqtt_client_publish(mqtt_client, topic.c_str(), 
                                        telemetry_data.c_str(), telemetry_data.length(), 
                                        1, 0);
    
    if (msg_id != -1) {
        ESP_LOGI(TAG, "Telemetry data sent successfully (T:%.2f°C, H:%.2f%%, P:%d, msg_id: %d)", temperature, humidity, power_status, msg_id);
    } else {
        ESP_LOGE(TAG, "Failed to send telemetry data");
    }
}

/**
 * @brief WiFi event handler for alert mode
 */
void AlertMode::wifi_event_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        esp_wifi_connect();
        xEventGroupClearBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "Got IP: " IPSTR, IP2STR(&event->ip_info.ip));
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
}

/**
 * @brief Connect to WiFi network
 */
void AlertMode::connect_wifi() {
    s_wifi_event_group = xEventGroupCreate();
    
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_sta();
    
    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));
    
    esp_event_handler_instance_t instance_any_id;
    esp_event_handler_instance_t instance_got_ip;
    ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT,
                                                        ESP_EVENT_ANY_ID,
                                                        &wifi_event_handler,
                                                        NULL,
                                                        &instance_any_id));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT,
                                                        IP_EVENT_STA_GOT_IP,
                                                        &wifi_event_handler,
                                                        NULL,
                                                        &instance_got_ip));
    
    wifi_config_t wifi_config = {};
    strcpy((char*)wifi_config.sta.ssid, app_config.ssid);
    strcpy((char*)wifi_config.sta.password, app_config.password);
    wifi_config.sta.threshold.authmode = WIFI_AUTH_WPA2_PSK;
    
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());
    
    ESP_LOGI(TAG, "WiFi init finished.");
    
    EventBits_t bits = xEventGroupWaitBits(s_wifi_event_group,
            WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
            pdFALSE,
            pdFALSE,
            portMAX_DELAY);
    
    if (bits & WIFI_CONNECTED_BIT) {
        ESP_LOGI(TAG, "Connected to AP SSID:%s", app_config.ssid);
    } else if (bits & WIFI_FAIL_BIT) {
        ESP_LOGI(TAG, "Failed to connect to SSID:%s", app_config.ssid);
    } else {
        ESP_LOGE(TAG, "UNEXPECTED EVENT");
    }
}

/**
 * @brief MQTT event handler for alert mode
 */
void AlertMode::mqtt_event_handler(void* handler_args, esp_event_base_t base, int32_t event_id, void* event_data) {
    esp_mqtt_event_handle_t event = static_cast<esp_mqtt_event_handle_t>(event_data);
    
    switch (event->event_id) {
        case MQTT_EVENT_CONNECTED:
            ESP_LOGI(TAG, "MQTT Connected");
            break;
            
        case MQTT_EVENT_PUBLISHED:
            ESP_LOGI(TAG, "Message published successfully");
            break;
            
        case MQTT_EVENT_ERROR:
            ESP_LOGE(TAG, "MQTT Error occurred");
            break;
            
        case MQTT_EVENT_DISCONNECTED:
            ESP_LOGI(TAG, "MQTT Disconnected");
            break;
            
        default:
            break;
    }
}

/**
 * @brief Connect to MQTT broker
 */
void AlertMode::connect_mqtt() {
    esp_mqtt_client_config_t mqtt_cfg = {};
    
    // Build MQTT broker URI
    std::string broker_uri = "mqtt://" + std::string(app_config.mqtt_server) + ":" + std::to_string(app_config.mqtt_port);
    mqtt_cfg.broker.address.uri = broker_uri.c_str();
    mqtt_cfg.credentials.username = app_config.mqtt_user;
    mqtt_cfg.credentials.authentication.password = app_config.mqtt_pass;
    mqtt_cfg.credentials.client_id = app_config.mqtt_client_id;
    
    mqtt_client = esp_mqtt_client_init(&mqtt_cfg);
    if (mqtt_client == NULL) {
        ESP_LOGE(TAG, "Failed to initialize MQTT client");
        return;
    }
    
    esp_mqtt_client_register_event(mqtt_client, MQTT_EVENT_ANY, mqtt_event_handler, this);
    esp_mqtt_client_start(mqtt_client);
    
    ESP_LOGI(TAG, "MQTT client started");
}

/**
 * @brief Get current power outage alert count from NVS
 * @return Number of alerts sent
 */
int AlertMode::getPowerOutageAlertCount() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error opening NVS handle: %s", esp_err_to_name(err));
        return 0;
    }
    
    int32_t alert_count = 0;
    err = nvs_get_i32(nvs_handle, "alert_count", &alert_count);
    if (err == ESP_ERR_NVS_NOT_FOUND) {
        alert_count = 0;
    } else if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error reading alert count: %s", esp_err_to_name(err));
        alert_count = 0;
    }
    
    nvs_close(nvs_handle);
    return alert_count;
}

void AlertMode::incrementPowerOutageAlertCount() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error opening NVS handle: %s", esp_err_to_name(err));
        return;
    }
    
    int32_t current_count = getPowerOutageAlertCount();
    current_count++;
    
    err = nvs_set_i32(nvs_handle, "alert_count", current_count);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error setting alert count: %s", esp_err_to_name(err));
    }
    
    err = nvs_commit(nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error committing NVS: %s", esp_err_to_name(err));
    }
    
    nvs_close(nvs_handle);
    ESP_LOGI(TAG, "Power outage alert count incremented to: %d", current_count);
}

void AlertMode::resetPowerOutageAlertCount() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error opening NVS handle: %s", esp_err_to_name(err));
        return;
    }
    
    err = nvs_set_i32(nvs_handle, "alert_count", 0);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error resetting alert count: %s", esp_err_to_name(err));
    }
    
    err = nvs_commit(nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error committing NVS: %s", esp_err_to_name(err));
    }
    
    nvs_close(nvs_handle);
    ESP_LOGI(TAG, "Power outage alert count reset to 0");
}

bool AlertMode::isMaxAlertsReached() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READONLY, &nvs_handle);
    if (err != ESP_OK) {
        return false;
    }
    
    uint8_t max_reached = 0;
    err = nvs_get_u8(nvs_handle, "max_reached", &max_reached);
    nvs_close(nvs_handle);
    
    return (err == ESP_OK && max_reached == 1);
}

void AlertMode::setMaxAlertsReached(bool reached) {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Error opening NVS handle: %s", esp_err_to_name(err));
        return;
    }
    
    uint8_t flag = reached ? 1 : 0;
    err = nvs_set_u8(nvs_handle, "max_reached", flag);
    if (err == ESP_OK) {
        nvs_commit(nvs_handle);
    }
    
    nvs_close(nvs_handle);
    ESP_LOGI(TAG, "Max alerts reached flag set to: %s", reached ? "true" : "false");
}