package org.quangdung.domain.use_case.interfaces;

import org.quangdung.domain.entity.MqttAccount;

import io.smallrye.mutiny.Uni;

public interface IMqttCreateAccountUseCase {
    Uni<MqttAccount> execute(String username, String deviceUuid);
}
