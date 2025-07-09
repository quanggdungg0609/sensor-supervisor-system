package org.quangdung.infrastructure.repository;


import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quangdung.core.exception.CheckClientIdExistsException;
import org.quangdung.core.exception.CheckMqttUsernameExistsException;
import org.quangdung.core.exception.FindByClientIdException;
import org.quangdung.core.exception.FindByMqttUsernameException;
import org.quangdung.core.exception.MqttAccountNotExistsException;
import org.quangdung.core.exception.ServiceCommunicationException;
import org.quangdung.core.exception.FindByMqttUsernameException;

import org.quangdung.core.metric.MetricService;
import org.quangdung.domain.entity.DeviceInfo;
import org.quangdung.domain.entity.MqttAccount;
import org.quangdung.domain.repository.IMqttRepository;
import org.quangdung.infrastructure.dao.DeviceDAO;
import org.quangdung.infrastructure.entity.DeviceDetailsResponse;
import org.quangdung.infrastructure.entity.MqttAccountEntity;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class MqttRepository implements IMqttRepository {
    private final Logger log; 
    private final MetricService metricsService;
    private final DeviceDAO deviceDAO;


    @Inject
    public MqttRepository(
        Logger log,
        MetricService metricsService,
        @RestClient DeviceDAO deviceDAO
    ) {
        this.log = log;
        this.metricsService = metricsService;
        this.deviceDAO = deviceDAO;
    }

    @Override
    public Uni<Boolean> checkMqttUsernameExists(String mqttUsername) {
        log.info("Checking Mqtt Id ....");
        
        return metricsService.timeOperation("mqtt.repository.check_username_exists", () -> {
            return MqttAccountEntity.count("mqttUsername", mqttUsername)
                .onItem().transform(count -> {
                    boolean exists = count > 0;
                    
                    // Record counter for username check results
                    metricsService.incrementCounter("mqtt.repository.username_check", 
                        "result=" + (exists ? "found" : "not_found"));
                    
                    return exists;
                })
                .onFailure().transform(throwable -> {
                    log.error(throwable);
                    
                    // Record error counter
                    metricsService.incrementCounter("mqtt.repository.username_check_errors", 
                        "error_type=" + throwable.getClass().getSimpleName());
                    
                    return new CheckMqttUsernameExistsException(throwable.getMessage(), throwable);
                });
        }, "operation=check_username_exists");
    }

    @Override
    public Uni<Boolean> checkClientIdExists(String clientId) {
        log.info("Checking Client ID ....");
        
        return metricsService.timeOperation("mqtt.repository.check_client_id_exists", () -> {
            return MqttAccountEntity.count("clientId", clientId)
                .onItem().transform(count -> {
                    boolean exists = count > 0;
                    
                    // Record counter for client ID check results
                    metricsService.incrementCounter("mqtt.repository.client_id_check", 
                        "result=" + (exists ? "found" : "not_found"));
                    
                    return exists;
                })
                .onFailure().transform(throwable -> {
                    log.error(throwable);
                    
                    // Record error counter
                    metricsService.incrementCounter("mqtt.repository.client_id_check_errors", 
                        "error_type=" + throwable.getClass().getSimpleName());
                    
                    throw new CheckClientIdExistsException(throwable.getMessage(), throwable);
                });
        }, "operation=check_client_id_exists");
    }

    @Override
    public Uni<MqttAccount> findByMqttUsername(String mqttUsername) {
        log.info("Finding MQTT account by mqttUsername: " + mqttUsername);
        
        return metricsService.timeOperation("mqtt.repository.find_by_username", () -> {
            return MqttAccountEntity.find("SELECT m FROM MqttAccountEntity m LEFT JOIN FETCH m.permissions WHERE m.mqttUsername = ?1", mqttUsername)
                .firstResult()
                .onItem().transform(entity -> {
                    MqttAccount result = (entity == null) ? null : MqttAccount.fromEntity((MqttAccountEntity) entity);
                    
                    // Record counter for find results
                    metricsService.incrementCounter("mqtt.repository.find_by_username", 
                        "result=" + (result != null ? "found" : "not_found"));
                    
                    return result;
                })
                .onFailure().transform(throwable -> {
                    log.error("Error finding MQTT account by mqttId", throwable);
                    
                    // Record error counter
                    metricsService.incrementCounter("mqtt.repository.find_by_username_errors", 
                        "error_type=" + throwable.getClass().getSimpleName());
                    
                    return new FindByMqttUsernameException(throwable.getMessage(), throwable);
                });
        }, "operation=find_by_username");
    }

    @Override
    public Uni<MqttAccount> findByClientId(String clientId) {
        log.info("Finding MQTT account by clientId: " + clientId);
        
        return metricsService.timeOperation("mqtt.repository.find_by_client_id", () -> {
            return MqttAccountEntity.find("SELECT m FROM MqttAccountEntity m LEFT JOIN FETCH m.permissions WHERE m.clientId = ?1", clientId)
                .firstResult()
                .onItem().transform(entity -> {
                    MqttAccount result = (entity == null) ? null : MqttAccount.fromEntity((MqttAccountEntity) entity);
                    
                    // Record counter for find results
                    metricsService.incrementCounter("mqtt.repository.find_by_client_id", 
                        "result=" + (result != null ? "found" : "not_found"));
                    
                    return result;
                })
                .onFailure().transform(throwable -> {
                    log.error("Error finding MQTT account by clientId", throwable);
                    
                    // Record error counter
                    metricsService.incrementCounter("mqtt.repository.find_by_client_id_errors", 
                        "error_type=" + throwable.getClass().getSimpleName());
                    
                    return new FindByClientIdException("Failed to find MQTT account", throwable);
                });
        }, "operation=find_by_client_id");
    }

    @Override
    public Uni<MqttAccount> createAccount(MqttAccount mqttAccount) {
        log.info("Saving account into DB...");
        
        return metricsService.timeOperation("mqtt.repository.create_account", () -> {
            MqttAccountEntity account = mqttAccount.toEntity();
            return account.persist().onItem().transform(savedAccount -> {
                
                // Record successful creation counter
                metricsService.incrementCounter("mqtt.repository.create_account", "result=success");
                
                return MqttAccount.fromEntity((MqttAccountEntity) savedAccount);
            })
            .onFailure().transform(throwable -> {
                log.error("Error saving account into DB", throwable);
                
                // Record error counter
                metricsService.incrementCounter("mqtt.repository.create_account_errors", 
                    "error_type=" + throwable.getClass().getSimpleName());
                
                return new FindByClientIdException("Failed to find MQTT account", throwable);
            });
        }, "operation=create_account");
    }

    @Override
    public Uni<DeviceInfo> getDeviceInfoByClientId(String clientId) {
         return metricsService.timeOperation("mqtt.repository.get_device_info_by_client_id", () -> {
            return MqttAccountEntity.<MqttAccountEntity>find("clientId", clientId).firstResult()
                .onItem().ifNull().failWith(new MqttAccountNotExistsException("Mqtt account not found for clientId: " + clientId))
                .onItem().ifNotNull().transformToUni(mqttAccountEntity -> { // Changed from transform to transformToUni
                    log.infof("Found MqttAccountEntity for clientId %s. Fetching device info from DeviceService...", clientId);
                    return deviceDAO.getDeviceByUuid(mqttAccountEntity.getDeviceUuid().toString())
                        .onItem().transform(deviceDetailsResponse -> {
                            log.infof("Received DeviceDetailsResponse for deviceUuid %s.", deviceDetailsResponse.getDeviceUuid());
                            metricsService.incrementCounter("mqtt.repository.get_device_info_by_client_id", "result=success");
                            return DeviceInfo.builder()
                                    .deviceUuid(deviceDetailsResponse.getDeviceUuid())
                                    .deviceName(deviceDetailsResponse.getDeviceName())
                                    .mqttUsername(deviceDetailsResponse.getMqttUsername())
                                    .clientId(clientId)
                                    .build();
                        });
                })
                .onFailure().transform(throwable -> { 
                        log.error(throwable);
                        metricsService.incrementCounter("mqtt.repository.get_device_info_by_client_id", "error_type=" + throwable.getClass().getSimpleName());
                        if (throwable instanceof MqttAccountNotExistsException) {
                            return throwable; 
                        } else if (throwable instanceof WebApplicationException) {
                            return throwable; 
                        } else {
                            return new ServiceCommunicationException(
                                String.format("An unexpected error occurred while fetching device info for clientId %s: %s", clientId, throwable.getMessage()),
                                throwable
                            );
                        }
            });
        }, "operation=get_device_info_by_client_id");
    }
}
