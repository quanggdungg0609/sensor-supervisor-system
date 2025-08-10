#ifndef CONFIGURATION_MODE_HPP
#define CONFIGURATION_MODE_HPP

#include "esp_http_server.h"

class ConfigurationMode {
public:
    ConfigurationMode() = default;
    void start();

private:
    void init_wifi_ap();
    void start_web_server();

    // Actual logic handler methods
    esp_err_t get_handler(httpd_req_t *req);
    esp_err_t post_save_handler(httpd_req_t *req);

    // Static callback handlers for C API compatibility
    static esp_err_t http_get_handler_static(httpd_req_t *req);
    static esp_err_t http_post_save_handler_static(httpd_req_t *req);
    
    /**
     * @brief URL decode a string (convert %XX to actual characters)
     * @param src Source string to decode
     * @param dst Destination buffer for decoded string
     * @param dst_size Size of destination buffer
     */
    void url_decode(const char* src, char* dst, size_t dst_size);
};

#endif // CONFIGURATION_MODE_HPP