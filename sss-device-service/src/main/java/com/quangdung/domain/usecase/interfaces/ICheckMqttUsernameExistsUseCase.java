package com.quangdung.domain.usecase.interfaces;

import io.smallrye.mutiny.Uni;

public interface ICheckMqttUsernameExistsUseCase {
    Uni<Boolean> execute(String mqttUsername);
}
