idf_component_register(SRCS "configuration_mode.cpp"
                    INCLUDE_DIRS "include"
                    REQUIRES
                        storage_manager
                        esp_http_server
                    PRIV_REQUIRES
                        esp_wifi
                        esp_event
                        esp_netif
)