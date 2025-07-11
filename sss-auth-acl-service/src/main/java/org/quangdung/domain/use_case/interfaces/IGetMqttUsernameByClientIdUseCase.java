package org.quangdung.domain.use_case.interfaces;

import io.smallrye.mutiny.Uni;

public interface IGetMqttUsernameByClientIdUseCase {
    Uni<String> execute(String clientId);
}
