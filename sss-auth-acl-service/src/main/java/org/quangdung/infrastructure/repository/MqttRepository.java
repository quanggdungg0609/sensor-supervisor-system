package org.quangdung.infrastructure.repository;

import org.jboss.logging.Logger;
import org.quangdung.core.exception.CheckClientIdExistsException;
import org.quangdung.core.exception.CheckMqttUsernameExistsException;
import org.quangdung.core.exception.FindByClientIdException;
import org.quangdung.core.exception.FindByMqttUsernameException;
import org.quangdung.domain.entity.MqttAccount;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.infrastructure.entity.MqttAccountEntity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttRepository implements IMqttRepository {
    private final Logger log; 

    @Inject
    public MqttRepository(Logger log) {
        this.log = log;
    }

    @Override
    public Uni<Boolean> checkMqttUsernameExists(String mqttUsername) {
        log.info("Checking Mqtt Id ....");
        return MqttAccountEntity.count("mqttUsername", mqttUsername)
            .onItem().transform(count -> count > 0)
            .onFailure().transform(throwable -> {
                log.error(throwable);
                return new CheckMqttUsernameExistsException(throwable.getMessage(), throwable);
            });
    }

    @Override
    public Uni<Boolean> checkClientIdExists(String clientId) {
         log.info("Checking Client ID ....");
         return MqttAccountEntity.count("clientId", clientId)
            .onItem().transform(count -> count > 0)
            .onFailure().transform(throwable -> {
                log.error(throwable);
                throw new CheckClientIdExistsException(throwable.getMessage(), throwable);
            });
    }

    @Override
    public Uni<MqttAccount> findByMqttUsername(String mqttUsername) {
        log.info("Finding MQTT account by mqttUsername: " + mqttUsername);
        return MqttAccountEntity.find("SELECT m FROM MqttAccountEntity m LEFT JOIN FETCH m.permissions WHERE m.mqttUsername = ?1", mqttUsername)
            .firstResult()
            .onItem().transform(entity -> {
                if (entity == null) {
                    return null;
                }
                return MqttAccount.fromEntity((MqttAccountEntity) entity);
            })
            .onFailure().transform(throwable -> {
                log.error("Error finding MQTT account by mqttId", throwable);
                return new FindByMqttUsernameException(throwable.getMessage(), throwable);
            });
    }

    @Override
    public Uni<MqttAccount> findByClientId(String clientId) {
        log.info("Finding MQTT account by clientId: " + clientId);
        return MqttAccountEntity.find("SELECT m FROM MqttAccountEntity m LEFT JOIN FETCH m.permissions WHERE m.clientId = ?1", clientId)
            .firstResult()
            .onItem().transform(entity -> {
                if (entity == null) {
                    return null;
                }
                return MqttAccount.fromEntity((MqttAccountEntity) entity);
            })
            .onFailure().transform(throwable -> {
                log.error("Error finding MQTT account by clientId", throwable);
                return new FindByClientIdException("Failed to find MQTT account", throwable);
            });
    }


    @Override
    public Uni<MqttAccount> createAccount(MqttAccount mqttAccount) {
        log.info("Saving account into DB...");
        MqttAccountEntity account = mqttAccount.toEntity();
        return account.persist().onItem().transform(savedAccount -> {
            return MqttAccount.fromEntity((MqttAccountEntity) savedAccount);
        })
        .onFailure().transform(throwable -> {
            log.error("Error saving account into DB", throwable);
            return new FindByClientIdException("Failed to find MQTT account", throwable);
        });
    }
}
