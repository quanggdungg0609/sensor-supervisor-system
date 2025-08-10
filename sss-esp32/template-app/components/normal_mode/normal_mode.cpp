#include "normal_mode.hpp"
#include "ntp_client.hpp"
#include "sensor_manager.hpp"
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

static const char* TAG = "NORMAL_MODE";
#define WIFI_CONNECTED_BIT BIT0
#define WIFI_FAIL_BIT      BIT1
static EventGroupHandle_t s_wifi_event_group;

NormalMode::NormalMode(const app_config_t& config) : app_config(config), mqtt_client(nullptr) {}

void NormalMode::start() {
    ESP_LOGI(TAG, "Starting Normal Mode...");
    
    connect_wifi();
    
    NTPClient::getInstance().initialize();

    if (!sensor_manager.init()) {
        ESP_LOGE(TAG, "Failed to initialize SensorManager! Restarting...");
        vTaskDelay(pdMS_TO_TICKS(2000));
        esp_restart();
    }

    connect_mqtt();
    
    // Configure power outage wake-up before entering deep sleep
    sensor_manager.configurePowerOutageWakeUp();
}

void NormalMode::mqtt_event_handler(void* handler_args, esp_event_base_t base, int32_t event_id, void* event_data) {
    esp_mqtt_event_handle_t event = (esp_mqtt_event_handle_t) event_data;
    NormalMode* normal_mode = static_cast<NormalMode*>(handler_args);
    
    switch ((esp_mqtt_event_id_t)event_id) {
    case MQTT_EVENT_CONNECTED: {
        ESP_LOGI(TAG, "MQTT_EVENT_CONNECTED");
        
        // Read sensor data and send telemetry
        float temperature = normal_mode->sensor_manager.getTemperature();
        float humidity = normal_mode->sensor_manager.getHumidity();
        int power_status = normal_mode->sensor_manager.getPowerStatus();
        
        // Create telemetry message with new format
        std::string timestamp = NTPClient::getInstance().getFormattedTimestamp();
        std::string telemetry_data = "{\"data\":{\"temperature\":" + std::to_string(temperature) + 
                                   ",\"humidity\":" + std::to_string(humidity) + 
                                   ",\"power_status\":" + std::to_string(power_status) + 
                                   "},\"timestamp\":\"" + timestamp + "\"}";
        
        // Create topic string with correct format: sensors/[clientId]/telemetry
        std::string topic = "sensors/" + std::string(normal_mode->app_config.mqtt_client_id) + "/telemetry";
        
        int msg_id = esp_mqtt_client_publish(normal_mode->mqtt_client, topic.c_str(), 
                                            telemetry_data.c_str(), telemetry_data.length(), 1, 0);
        
        if (msg_id != -1) {
            ESP_LOGI(TAG, "Telemetry data sent successfully (T:%.2f°C, H:%.2f%%, P:%d, msg_id: %d)", temperature, humidity, power_status, msg_id);
        } else {
            ESP_LOGE(TAG, "Failed to send telemetry data");
        }
        
        // Wait for message to be sent, then go to deep sleep
        vTaskDelay(pdMS_TO_TICKS(2000));
        
        // Use a default sleep duration since it's not in config
        int sleep_minutes = 5; // Default 5 minutes
        ESP_LOGI(TAG, "Entering deep sleep for %d minutes...", sleep_minutes);
        esp_deep_sleep(sleep_minutes * 60 * 1000000ULL);
        break;
    }
    case MQTT_EVENT_DISCONNECTED:
        ESP_LOGI(TAG, "MQTT_EVENT_DISCONNECTED");
        break;
        
    case MQTT_EVENT_SUBSCRIBED:
        ESP_LOGI(TAG, "MQTT_EVENT_SUBSCRIBED, msg_id=%d", event->msg_id);
        break;
        
    case MQTT_EVENT_UNSUBSCRIBED:
        ESP_LOGI(TAG, "MQTT_EVENT_UNSUBSCRIBED, msg_id=%d", event->msg_id);
        break;
        
    case MQTT_EVENT_PUBLISHED:
        ESP_LOGI(TAG, "MQTT_EVENT_PUBLISHED, msg_id=%d", event->msg_id);
        break;
        
    case MQTT_EVENT_DATA:
        ESP_LOGI(TAG, "MQTT_EVENT_DATA");
        break;
        
    case MQTT_EVENT_ERROR:
        ESP_LOGI(TAG, "MQTT_EVENT_ERROR");
        break;
        
    default:
        ESP_LOGI(TAG, "Other event id:%d", event->event_id);
        break;
    }
}

void NormalMode::connect_mqtt() {
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

void NormalMode::connect_wifi() {
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
 * @brief WiFi event handler for connection management
 * @param arg User data passed to the handler
 * @param event_base Event base for the handler
 * @param event_id Event ID
 * @param event_data Event data
 */
void NormalMode::wifi_event_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        esp_wifi_connect();
        ESP_LOGI(TAG, "Retry to connect to the AP");
        xEventGroupClearBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "Got IP:" IPSTR, IP2STR(&event->ip_info.ip));
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
}

/**
 * @brief Get power outage alert count from NVS storage
 * @return Number of times power outage alert has been sent
 */
int NormalMode::getPowerOutageAlertCount() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READONLY, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "Failed to open NVS for reading power outage count");
        return 0;
    }
    
    int32_t count = 0;
    err = nvs_get_i32(nvs_handle, "alert_count", &count);
    if (err != ESP_OK && err != ESP_ERR_NVS_NOT_FOUND) {
        ESP_LOGW(TAG, "Failed to read power outage alert count from NVS");
    }
    
    nvs_close(nvs_handle);
    return (int)count;
}

/**
 * @brief Increment power outage alert count in NVS storage
 */
void NormalMode::incrementPowerOutageAlertCount() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to open NVS for writing power outage count");
        return;
    }
    
    int current_count = getPowerOutageAlertCount();
    int32_t new_count = current_count + 1;
    
    err = nvs_set_i32(nvs_handle, "alert_count", new_count);
    if (err != ESP_OK) {
        ESP_LOGE(TAG, "Failed to write power outage alert count to NVS");
    } else {
        err = nvs_commit(nvs_handle);
        if (err != ESP_OK) {
            ESP_LOGE(TAG, "Failed to commit power outage alert count to NVS");
        } else {
            ESP_LOGI(TAG, "Power outage alert count updated to: %d", (int)new_count);
        }
    }
    
    nvs_close(nvs_handle);
}

/**
 * @brief Reset power outage alert count in NVS storage
 */
void NormalMode::resetPowerOutageAlertCount() {
    nvs_handle_t nvs_handle;
    esp_err_t err = nvs_open("power_outage", NVS_READWRITE, &nvs_handle);
    if (err != ESP_OK) {
        ESP_LOGW(TAG, "Failed to open NVS for resetting power outage count");
        return;
    }
    
    err = nvs_erase_key(nvs_handle, "alert_count");
    if (err != ESP_OK && err != ESP_ERR_NVS_NOT_FOUND) {
        ESP_LOGW(TAG, "Failed to reset power outage alert count in NVS");
    } else {
        err = nvs_commit(nvs_handle);
        if (err == ESP_OK) {
            ESP_LOGI(TAG, "Power outage alert count reset to 0");
        }
    }
    
    nvs_close(nvs_handle);
}