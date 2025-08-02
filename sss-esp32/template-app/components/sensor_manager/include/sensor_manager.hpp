#ifndef SENSOR_MANAGER_HPP
#define SENSOR_MANAGER_HPP

#include <string>
#include "max31865.h" 

class SensorManager {
public:
    SensorManager() = default;
    bool init();
    std::string getAggregatedDataJson();

private:
    max31865_t dev;

    float getTemperature();
    float getHumidity();
};
#endif // SENSOR_MANAGER_HPP