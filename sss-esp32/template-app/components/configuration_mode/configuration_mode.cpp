#include "configuration_mode.hpp"
#include "storage_manager.hpp"

#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "esp_system.h"
#include "esp_wifi.h"
#include "esp_event.h"
#include "esp_log.h"

#include <string>
#include <vector>
#include <algorithm>

#define AP_SSID "ESP32-Config-CPP"
#define AP_PASS "" 
#define AP_CHANNEL 1
#define AP_MAX_CONN 4

static const char* TAG = "CONFIG_MODE_CPP";

// --- Public Methods ---
void ConfigurationMode::start() {
    ESP_LOGI(TAG, "Starting C++ Configuration Mode...");
    init_wifi_ap();
    start_web_server();
}

// --- Private Methods ---
void ConfigurationMode::init_wifi_ap() {
    ESP_ERROR_CHECK(esp_netif_init());
    ESP_ERROR_CHECK(esp_event_loop_create_default());
    esp_netif_create_default_wifi_ap();

    wifi_init_config_t cfg = WIFI_INIT_CONFIG_DEFAULT();
    ESP_ERROR_CHECK(esp_wifi_init(&cfg));

    wifi_config_t wifi_config = {};
    strcpy((char*)wifi_config.ap.ssid, AP_SSID);
    wifi_config.ap.ssid_len = strlen(AP_SSID);
    strcpy((char*)wifi_config.ap.password, AP_PASS);
    wifi_config.ap.channel = AP_CHANNEL;
    wifi_config.ap.max_connection = AP_MAX_CONN;
    wifi_config.ap.authmode = WIFI_AUTH_OPEN;
    
    ESP_ERROR_CHECK(esp_wifi_set_mode(WIFI_MODE_APSTA));
    ESP_ERROR_CHECK(esp_wifi_set_config(WIFI_IF_AP, &wifi_config));
    ESP_ERROR_CHECK(esp_wifi_start());

    ESP_LOGI(TAG, "WiFi SoftAP started. SSID: %s", AP_SSID);
}

void ConfigurationMode::start_web_server() {
    httpd_config_t config = HTTPD_DEFAULT_CONFIG();
    config.lru_purge_enable = true;
    httpd_handle_t server = NULL;

    ESP_LOGI(TAG, "Starting web server on port: '%d'", config.server_port);
    if (httpd_start(&server, &config) == ESP_OK) {
        httpd_uri_t get_uri = {
            .uri       = "/",
            .method    = HTTP_GET,
            .handler   = http_get_handler_static,
            .user_ctx  = this // Pass the class instance context
        };
        httpd_register_uri_handler(server, &get_uri);

        httpd_uri_t post_uri = {
            .uri       = "/save",
            .method    = HTTP_POST,
            .handler   = http_post_save_handler_static,
            .user_ctx  = this // Pass the class instance context
        };
        httpd_register_uri_handler(server, &post_uri);
    } else {
        ESP_LOGE(TAG, "Error starting server!");
    }
}

// --- Static Handlers (C-style callbacks) ---
esp_err_t ConfigurationMode::http_get_handler_static(httpd_req_t *req) {
    ConfigurationMode* self = static_cast<ConfigurationMode*>(req->user_ctx);
    return self->get_handler(req);
}

esp_err_t ConfigurationMode::http_post_save_handler_static(httpd_req_t *req) {
    ConfigurationMode* self = static_cast<ConfigurationMode*>(req->user_ctx);
    return self->post_save_handler(req);
}

// --- Member function handlers (Actual logic) ---
esp_err_t ConfigurationMode::get_handler(httpd_req_t *req) {
    std::string wifi_options_html;

    // Scan WiFi
    ESP_LOGI(TAG, "Scanning WiFi networks...");
    wifi_scan_config_t scan_config = { .ssid = 0, .bssid = 0, .channel = 0, .show_hidden = false };
    ESP_ERROR_CHECK(esp_wifi_scan_start(&scan_config, true));
    
    uint16_t num_networks = 0;
    esp_wifi_scan_get_ap_num(&num_networks);
    if (num_networks > 0) {
        std::vector<wifi_ap_record_t> ap_records(num_networks);
        ESP_ERROR_CHECK(esp_wifi_scan_get_ap_records(&num_networks, ap_records.data()));
        for (const auto& record : ap_records) {
            char option[128];
            snprintf(option, sizeof(option), "<option value='%s'>%s (%d dBm)</option>", 
                     (char*)record.ssid, (char*)record.ssid, record.rssi);
            wifi_options_html += option;
        }
    } else {
        wifi_options_html = "<option value=''>No networks found</option>";
    }

    // Using std::string for safer HTML construction
    std::string html = R"rawliteral(
      <!DOCTYPE HTML><html><head>
      <title>Configuration ESP32</title>
      <meta charset="UTF-8"><meta name="viewport" content="width=device-width, initial-scale=1">
      <style>body{font-family:Arial,sans-serif;background-color:#f4f4f4;margin:0;padding:20px}.container{background-color:#fff;padding:20px;border-radius:8px;box-shadow:0 2px 4px rgba(0,0,0,0.1);max-width:500px;margin:auto}h2{color:#333}label{display:block;margin-top:10px;color:#555}input[type=text],input[type=password],select{width:calc(100% - 22px);padding:10px;margin-top:5px;border:1px solid #ddd;border-radius:4px}input[type=submit]{background-color:#007BFF;color:white;padding:14px 20px;margin-top:20px;border:none;border-radius:4px;cursor:pointer;width:100%;font-size:16px}input[type=submit]:hover{background-color:#0056b3}</style>
      </head><body><div class="container">
      <h2>Configuration ESP32</h2><form action="/save" method="POST">
      <label for="device_name">Nom de l'appareil</label><input type="text" id="device_name" name="device_name" placeholder="Ex: Capteur Salon"><hr><h3>Paramètres WiFi</h3>
      <label for="wifi_select">Choisissez un réseau WiFi :</label><select id="wifi_select" name="wifi_select" onchange="document.getElementById('ssid').value = this.value">
      <option value="">--Veuillez sélectionner--</option>)rawliteral";
    
    html += wifi_options_html;

    html += R"rawliteral(</select>
      <label for="ssid">Ou entrez le nom du WiFi (SSID)</label><input type="text" id="ssid" name="ssid">
      <label for="pass">Mot de passe WiFi</label><input type="password" id="pass" name="pass"><hr><h3>Paramètres du Broker MQTT</h3>
      <label for="mqtt_server">Adresse du Serveur</label><input type="text" id="mqtt_server" name="mqtt_server">
      <label for="mqtt_port">Port</label><input type="text" id="mqtt_port" name="mqtt_port" value="1883">
      <label for="mqtt_user">Nom d'utilisateur</label><input type="text" id="mqtt_user" name="mqtt_user">
      <label for="mqtt_pass">Mot de passe</label><input type="password" id="mqtt_pass" name="mqtt_pass">
      <label for="mqtt_client_id">Client ID (Obligatoire)</label><input type="text" id="mqtt_client_id" name="mqtt_client_id" placeholder="Ex: esp32-salon-01" required>
      <input type="submit" value="Sauvegarder et Redémarrer"></form></div></body></html>)rawliteral";

    httpd_resp_set_type(req, "text/html");
    httpd_resp_send(req, html.c_str(), html.length());
    
    return ESP_OK;
}

/**
 * @brief URL decode a string (convert %XX to actual characters)
 * @param src Source string to decode
 * @param dst Destination buffer for decoded string
 * @param dst_size Size of destination buffer
 */
void ConfigurationMode::url_decode(const char* src, char* dst, size_t dst_size) {
    size_t src_len = strlen(src);
    size_t dst_idx = 0;
    
    for (size_t i = 0; i < src_len && dst_idx < dst_size - 1; i++) {
        if (src[i] == '%' && i + 2 < src_len) {
            // Convert hex to char
            char hex[3] = {src[i+1], src[i+2], '\0'};
            char* endptr;
            long val = strtol(hex, &endptr, 16);
            if (*endptr == '\0') {
                dst[dst_idx++] = (char)val;
                i += 2; // Skip the hex digits
            } else {
                dst[dst_idx++] = src[i]; // Keep original if not valid hex
            }
        } else if (src[i] == '+') {
            dst[dst_idx++] = ' '; // Convert + to space
        } else {
            dst[dst_idx++] = src[i];
        }
    }
    dst[dst_idx] = '\0';
}

esp_err_t ConfigurationMode::post_save_handler(httpd_req_t *req) {
    std::vector<char> buf(req->content_len + 1);
    int ret = httpd_req_recv(req, buf.data(), req->content_len);
    if (ret <= 0) { return ESP_FAIL; }
    buf[ret] = '\0';

    app_config_t cfg = {}; // Initialize struct with zeros
    char port_str[6] = "1883";

    // Helper lambda to safely copy and URL decode form values
    auto get_val = [&](const char* key, char* dest, size_t max_len) {
        char val[128];
        if (httpd_query_key_value(buf.data(), key, val, sizeof(val)) == ESP_OK) {
            // URL decode the value before copying
            url_decode(val, dest, max_len);
        }
    };
    
    get_val("device_name", cfg.device_name, sizeof(cfg.device_name));
    get_val("ssid", cfg.ssid, sizeof(cfg.ssid));
    get_val("pass", cfg.password, sizeof(cfg.password));
    get_val("mqtt_server", cfg.mqtt_server, sizeof(cfg.mqtt_server));
    get_val("mqtt_port", port_str, sizeof(port_str));
    get_val("mqtt_user", cfg.mqtt_user, sizeof(cfg.mqtt_user));
    get_val("mqtt_pass", cfg.mqtt_pass, sizeof(cfg.mqtt_pass));
    get_val("mqtt_client_id", cfg.mqtt_client_id, sizeof(cfg.mqtt_client_id));
    cfg.mqtt_port = atoi(port_str);

    // Use the Singleton to save the config
    StorageManager::getInstance().save_config(cfg);

    // Respond and restart
    const char* resp_str = "<h2>Configuration sauvegardée !</h2><p>L'appareil va redémarrer...</p>";
    httpd_resp_send(req, resp_str, strlen(resp_str));
    
    vTaskDelay(pdMS_TO_TICKS(3000));
    esp_restart();

    return ESP_OK;
}
