package org.quangdung.core.init;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.reactive.mutiny.Mutiny.SessionFactory;
import org.jboss.logging.Logger;
import org.quangdung.core.utils.password_util.IPasswordUtil;
import org.quangdung.infrastructure.entity.MqttAccountEntity;
import org.quangdung.infrastructure.entity.MqttPermissionEntity;

import io.quarkus.runtime.StartupEvent;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

@ApplicationScoped
public class MqttAdminInitialization {
    private final Logger log;
    private final IPasswordUtil passwordUtil;
    private final SessionFactory sessionFactory;

    @ConfigProperty(name = "mqtt.admin.username")
    private String adminUsername;
    @ConfigProperty(name = "mqtt.admin.password")
    private String adminPassword;
    @ConfigProperty(name = "mqtt.admin.clientid")
    private String adminClientId;




    @Inject
    public MqttAdminInitialization(Logger log, IPasswordUtil passwordUtil, SessionFactory sessionFactory) {
        this.log = log;
        this.passwordUtil = passwordUtil;
        this.sessionFactory = sessionFactory;
    }
    

    /**
     * Initializes the admin account on application startup.
     * Checks if admin account exists, creates one if not found.
     * 
     * @param ev the startup event
     */
    void onStart(@Observes StartupEvent ev){
        log.info("Checking for admin account using Mutiny.SessionFactory...");
        this.sessionFactory.<Void>withTransaction((session, tx) ->
            session.createQuery("FROM MqttAccountEntity WHERE mqttUsername = :username", MqttAccountEntity.class)
                .setParameter("username", adminUsername)
                .getSingleResultOrNull()
                .onItem().transformToUni(existingAdmin -> {
                    if (existingAdmin != null) {
                        log.info("Admin account '" + adminUsername + "' already exists. Skipping creation.");
                        return Uni.createFrom().voidItem();
                    } else {
                        log.info("Admin account not found. Creating new admin account...");
                        MqttAccountEntity newAdminAccount = buildAdminAccount();
                        return session.persist(newAdminAccount);
                    }
                })
        )
        .await().indefinitely();
        log.info("Admin initialization check finished.");
    }

    /**
     * Builds a new admin account with default permissions.
     * 
     * @return MqttAccountEntity the configured admin account
     */
    private MqttAccountEntity buildAdminAccount() {
        MqttAccountEntity adminAccount = MqttAccountEntity.builder()
            .mqttUsername(adminUsername)
            .clientId(adminClientId)
            .mqttPassword(passwordUtil.hash(adminPassword))
            .build();

        MqttPermissionEntity publishPermission = MqttPermissionEntity.builder()
            .topicPattern("#")
            .action(MqttPermissionEntity.MqttAction.PUBLISH)
            .permission(MqttPermissionEntity.MqttPermission.ALLOW)
            .build();

        MqttPermissionEntity subscribePermission = MqttPermissionEntity.builder()
            .topicPattern("#")
            .action(MqttPermissionEntity.MqttAction.SUBSCRIBE)
            .permission(MqttPermissionEntity.MqttPermission.ALLOW)
            .build();

        adminAccount.addPermission(publishPermission);
        adminAccount.addPermission(subscribePermission);
        return adminAccount;
    }
}
