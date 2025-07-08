package com.quangdung.domain.usecase.implement;

import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.domain.usecase.interfaces.ICheckMqttUsernameExistsUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class CheckMqttUsernameExists implements ICheckMqttUsernameExistsUseCase {
    private final IDeviceRepository deviceRepository;

    @Inject
    public CheckMqttUsernameExists(IDeviceRepository deviceRepository) {
        this.deviceRepository = deviceRepository;
    }

    @Override
    public Uni<Boolean>execute(String mqttUsername) {
        return deviceRepository.isMqttUsernameExist(mqttUsername);
    }

}
