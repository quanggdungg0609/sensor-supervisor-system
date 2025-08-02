idf_component_register(SRCS "main.cpp"
                    INCLUDE_DIRS "."
                    REQUIRES 
                        configuration_mode
                        normal_mode
                        storage_manager
                    PRIV_REQUIRES 
                        nvs_flash
                        driver
)