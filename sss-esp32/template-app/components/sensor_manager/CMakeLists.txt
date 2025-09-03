idf_component_register(SRCS "sensor_manager.cpp"
                    INCLUDE_DIRS "include"
                    REQUIRES ntp_client arduinojson pt100 power_outage_detector)
