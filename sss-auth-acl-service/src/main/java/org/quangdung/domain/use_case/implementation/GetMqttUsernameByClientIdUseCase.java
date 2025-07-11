package org.quangdung.domain.use_case.implementation;

import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.IGetMqttUsernameByClientIdUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class GetMqttUsernameByClientIdUseCase implements IGetMqttUsernameByClientIdUseCase {
    @Inject
    private IMqttRepository mqttRepository;

    @Override
    public Uni<String> execute(String clientId) {
        return mqttRepository.getMqttUsernameByClientId(clientId);
    }

}
    

