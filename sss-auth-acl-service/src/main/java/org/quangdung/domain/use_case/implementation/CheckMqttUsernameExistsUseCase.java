package org.quangdung.domain.use_case.implementation;

import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.domain.use_case.interfaces.ICheckMqttUsernameExistsUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CheckMqttUsernameExistsUseCase implements ICheckMqttUsernameExistsUseCase {
    private final IMqttRepository mqttRepository;

    @Inject
    public CheckMqttUsernameExistsUseCase(IMqttRepository mqttRepository) {
        this.mqttRepository = mqttRepository;
    }

    @Override
    public Uni<Boolean> execute(String mqttUsername) {
        return mqttRepository.checkMqttUsernameExists(mqttUsername);
    }
    
}
