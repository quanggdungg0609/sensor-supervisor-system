package org.quangdung.domain.repository;

import org.quangdung.domain.entity.MqttAccount;

import io.smallrye.mutiny.Uni;

public interface IMqttRepository {
    Uni<Boolean> checkMqttUsernameExists(String mqttUsername);
    Uni<Boolean> checkClientIdExists(String clientId);
    Uni<MqttAccount> createAccount(MqttAccount mqttAccount);
    Uni<MqttAccount> findByMqttUsername(String mqttUsername);
    Uni<MqttAccount> findByClientId(String clientId);
}
