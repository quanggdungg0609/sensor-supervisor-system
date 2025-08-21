#include "ntp_client.hpp"
#include "esp_sntp.h"
#include "esp_log.h"
#include "freertos/FreeRTOS.h"
#include "freertos/task.h"
#include "time.h"

static const char* TAG = "NTP_CLIENT";

NTPClient& NTPClient::getInstance() {
    static NTPClient instance;
    return instance;
}

void time_sync_notification_cb(struct timeval *tv) {
    ESP_LOGI(TAG, "Time synchronized");
}

void NTPClient::initialize() {
    ESP_LOGI(TAG, "Initializing SNTP");
    esp_sntp_setoperatingmode(SNTP_OPMODE_POLL);
    esp_sntp_setservername(0, "pool.ntp.org");
    sntp_set_time_sync_notification_cb(time_sync_notification_cb);
    esp_sntp_init();

    time_t now = 0;
    struct tm timeinfo = { 0 };
    int retry = 0;
    const int retry_count = 10;
    while (sntp_get_sync_status() == SNTP_SYNC_STATUS_RESET && ++retry < retry_count) {
        ESP_LOGI(TAG, "Waiting for system time to be set... (%d/%d)", retry, retry_count);
        vTaskDelay(2000 / portTICK_PERIOD_MS);
    }
}

/**
 * @brief Get formatted timestamp in ISO 8601 format with UTC timezone indicator
 * @return Formatted timestamp string in format "YYYY-MM-DDTHH:MM:SSZ"
 */
std::string NTPClient::getFormattedTimestamp() {
    time_t now;
    char strftime_buf[64];
    struct tm timeinfo;

    time(&now);
    gmtime_r(&now, &timeinfo); // Use GMT/UTC time instead of local time

    strftime(strftime_buf, sizeof(strftime_buf), "%Y-%m-%dT%H:%M:%SZ", &timeinfo);
    return std::string(strftime_buf);
}