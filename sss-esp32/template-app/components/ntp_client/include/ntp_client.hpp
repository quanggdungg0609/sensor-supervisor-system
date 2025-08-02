#ifndef NTP_CLIENT_HPP
#define NTP_CLIENT_HPP

#include <string>

class NTPClient {
public:
    static NTPClient& getInstance();
    void initialize();
    std::string getFormattedTimestamp();

    NTPClient(const NTPClient&) = delete;
    void operator=(const NTPClient&) = delete;
private:
    NTPClient() = default;
};

#endif // NTP_CLIENT_HPP