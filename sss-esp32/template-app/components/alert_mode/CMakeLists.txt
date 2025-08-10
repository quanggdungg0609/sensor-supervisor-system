idf_component_register(SRCS "alert_mode.cpp"
                    INCLUDE_DIRS "include"
                    REQUIRES
                        storage_manager
                        esp_wifi
                        esp_event
                        sensor_manager
                        mqtt
                        nvs_flash
                        esp_hw_support)