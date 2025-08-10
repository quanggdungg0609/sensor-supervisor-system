idf_component_register(SRCS "sensor_manager.cpp"
                    INCLUDE_DIRS "include"
                    REQUIRES ntp_client arduinojson sht30 power_outage_detector)
