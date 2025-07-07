package org.quangdung.domain.use_case.interfaces;

import io.smallrye.mutiny.Uni;

public interface ICheckMqttUsernameExistsUseCase {
    Uni<Boolean> execute(String mqttUsername);
}
