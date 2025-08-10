idf_component_register(SRCS "normal_mode.cpp"
                    INCLUDE_DIRS "include"
                    REQUIRES
                        storage_manager
                        ntp_client
                        esp_wifi
                        esp_event
                        sensor_manager
                        mqtt
                        nvs_flash
)