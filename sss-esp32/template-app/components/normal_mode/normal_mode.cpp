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
#include "mqtt_client.h"
#include <string>

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
}

void NormalMode::wifi_event_handler(void* arg, esp_event_base_t event_base, int32_t event_id, void* event_data) {
    if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_START) {
        esp_wifi_connect();
    } else if (event_base == WIFI_EVENT && event_id == WIFI_EVENT_STA_DISCONNECTED) {
        xEventGroupSetBits(s_wifi_event_group, WIFI_FAIL_BIT);
        ESP_LOGI(TAG, "connect to the AP fail");
    } else if (event_base == IP_EVENT && event_id == IP_EVENT_STA_GOT_IP) {
        ip_event_got_ip_t* event = (ip_event_got_ip_t*) event_data;
        ESP_LOGI(TAG, "got ip:" IPSTR, IP2STR(&event->ip_info.ip));
        xEventGroupSetBits(s_wifi_event_group, WIFI_CONNECTED_BIT);
    }
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
    ESP_ERROR_CHECK(esp_event_handler_instance_register(WIFI_EVENT, ESP_EVENT_ANY_ID, &wifi_event_handler, NULL, &instance_any_id));
    ESP_ERROR_CHECK(esp_event_handler_instance_register(IP_EVENT, IP_EVENT_STA_GOT_IP, &wifi_event_handler, NULL, &instance_got_ip));

    wifi_config_t wifi_config = {};
    strncpy((char*)wifi_config.sta.ssid, app_config.ssid, sizeof(wifi_config.sta.ssid));
    strncpy((char*)wifi_config.sta.password, app_config.password, sizeof(wifi_config.sta.password));
    
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_STA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_STA, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(TAG, "Connecting to WiFi SSID: %s", app_config.ssid);

    EventBits_t bits = xEventGroupWaitBits(s_wifi_event_group,
            WIFI_CONNECTED_BIT | WIFI_FAIL_BIT,
            pdFALSE,
            pdFALSE,
            portMAX_DELAY);

    if (bits & WIFI_CONNECTED_BIT) {
        ESP_LOGI(TAG, "Connected to WiFi!");
    } else if (bits & WIFI_FAIL_BIT) {
        ESP_LOGW(TAG, "Failed to connect to WiFi. Restarting...");
        vTaskDelay(pdMS_TO_TICKS(5000));
        esp_restart();
    }
}

void NormalMode::mqtt_event_handler(void* handler_args, esp_event_base_t base, int32_t event_id, void* event_data) {
    NormalMode* self = static_cast<NormalMode*>(handler_args);
    esp_mqtt_event_handle_t event = (esp_mqtt_event_handle_t)event_data;
    
    switch ((esp_mqtt_event_id_t)event_id) {
        case MQTT_EVENT_CONNECTED:
            ESP_LOGI(TAG, "MQTT_EVENT_CONNECTED");
            // Publish status and subscribe to command topic
            {
                std::string status_topic = "sensors/" + std::string(self->app_config.mqtt_client_id) + "/status";
                esp_mqtt_client_publish(self->mqtt_client, status_topic.c_str(), "{\"status\":\"active\"}", 0, 1, true);
                
                std::string cmd_topic = "sensors/" + std::string(self->app_config.mqtt_client_id) + "/command";
                esp_mqtt_client_subscribe(self->mqtt_client, cmd_topic.c_str(), 0);
            }
            // Get sensor data and publish
            {
                std::string payload = self->sensor_manager.getAggregatedDataJson();
                std::string telemetry_topic = "sensors/" + std::string(self->app_config.mqtt_client_id) + "/telemetry";
                esp_mqtt_client_publish(self->mqtt_client, telemetry_topic.c_str(), payload.c_str(), 0, 1, false);
                ESP_LOGI(TAG, "Telemetry sent: %s", payload.c_str());
            }
            // Disconnect and go to deep sleep
            ESP_LOGI(TAG, "Work done. Entering deep sleep...");
            esp_mqtt_client_disconnect(self->mqtt_client);
            esp_sleep_enable_timer_wakeup(10 * 1000000); // Sleep for 10 seconds
            esp_deep_sleep_start();
            break;
        case MQTT_EVENT_DISCONNECTED:
            ESP_LOGI(TAG, "MQTT_EVENT_DISCONNECTED");
            break;
        case MQTT_EVENT_DATA:
            ESP_LOGI(TAG, "MQTT_EVENT_DATA");
            printf("TOPIC=%.*s\r\n", event->topic_len, event->topic);
            printf("DATA=%.*s\r\n", event->data_len, event->data);
            break;
        case MQTT_EVENT_ERROR:
            ESP_LOGE(TAG, "MQTT_EVENT_ERROR");
            break;
        default:
            break;
    }
}

void NormalMode::connect_mqtt() {
   std::string lwt_topic = "sensors/" + std::string(app_config.mqtt_client_id) + "/status";
    
    esp_mqtt_client_config_t mqtt_cfg = {};


    std::string server_address(app_config.mqtt_server);
    std::string final_uri;

    if (server_address.find("://") == std::string::npos) {
        final_uri = "mqtt://" + server_address;
        ESP_LOGI(TAG, "MQTT scheme not found, adding default 'mqtt://'. Final URI: %s", final_uri.c_str());
    } else {
        final_uri = server_address;
        ESP_LOGI(TAG, "MQTT URI already includes a scheme. Using: %s", final_uri.c_str());
    }

    mqtt_cfg.broker.address.uri = final_uri.c_str();
    
    
    mqtt_cfg.broker.address.port = app_config.mqtt_port;
    mqtt_cfg.credentials.username = app_config.mqtt_user;
    mqtt_cfg.credentials.client_id = app_config.mqtt_client_id;
    mqtt_cfg.credentials.authentication.password = app_config.mqtt_pass;
    mqtt_cfg.session.last_will.topic = lwt_topic.c_str();
    mqtt_cfg.session.last_will.msg = "{\"status\":\"inactive\"}";
    mqtt_cfg.session.last_will.retain = true;

    mqtt_client = esp_mqtt_client_init(&mqtt_cfg);
    esp_mqtt_client_register_event(mqtt_client, (esp_mqtt_event_id_t)ESP_EVENT_ANY_ID, mqtt_event_handler, this);
    esp_mqtt_client_start(mqtt_client);
}