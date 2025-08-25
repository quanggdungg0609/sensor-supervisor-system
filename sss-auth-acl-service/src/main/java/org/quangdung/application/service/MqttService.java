package org.quangdung.application.service;

import java.util.Map;

import org.jboss.logging.Logger;
import org.quangdung.application.dto.request.MqttAclRequest;
import org.quangdung.application.dto.request.MqttAuthRequest;
import org.quangdung.application.dto.request.MqttCreateAccountRequest;
import org.quangdung.application.dto.response.ChangePasswordResponse;
import org.quangdung.application.dto.response.MqttAccountInfoWithPass;
import org.quangdung.application.dto.response.MqttResponse;
import org.quangdung.application.dto.response.MqttUsernameRes;
import org.quangdung.domain.use_case.interfaces.IChangePasswordUseCase;
import org.quangdung.domain.use_case.interfaces.IGetDeviceDetailsByClientIdUseCase;
import org.quangdung.domain.use_case.interfaces.IGetMqttUsernameByClientIdUseCase;
import org.quangdung.domain.use_case.interfaces.IMqttAuthenticationUseCase;
import org.quangdung.domain.use_case.interfaces.IMqttAuthorizationUseCase;
import org.quangdung.domain.use_case.interfaces.IMqttCreateAccountUseCase;

import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class MqttService {
    private final Logger log;
    private final IMqttAuthenticationUseCase authenticationUseCase;
    private final IMqttAuthorizationUseCase authorizationUseCase;
    private final IMqttCreateAccountUseCase createAccountUseCase;
    private final IGetDeviceDetailsByClientIdUseCase getDeviceDetailsByClientIdUseCase;
    private final IGetMqttUsernameByClientIdUseCase getMqttUsernameByClientIdUseCase;
    private final IChangePasswordUseCase changePasswordUseCase;

    @Inject
    public MqttService(
        Logger log,
        IMqttAuthenticationUseCase authenticationUseCase,
        IMqttAuthorizationUseCase authorizationUseCase,
        IMqttCreateAccountUseCase createAccountUseCase, 
        IGetDeviceDetailsByClientIdUseCase getDeviceDetailsByClientIdUseCase,
        IGetMqttUsernameByClientIdUseCase getMqttUsernameByClientIdUseCase,
        IChangePasswordUseCase changePasswordUseCase
    ) {
        this.log = log;
        this.authenticationUseCase = authenticationUseCase;
        this.authorizationUseCase = authorizationUseCase;
        this.createAccountUseCase = createAccountUseCase;
        this.getDeviceDetailsByClientIdUseCase = getDeviceDetailsByClientIdUseCase;
        this.getMqttUsernameByClientIdUseCase = getMqttUsernameByClientIdUseCase;
        this.changePasswordUseCase = changePasswordUseCase;
    }

    public Uni<Response> createNewAccount(MqttCreateAccountRequest request){
        log.info("Processing MQTT account creation request for username: " + request.getMqttUsername());
        return createAccountUseCase.execute(request.getMqttUsername(), request.getDeviceUuid()).onItem().transform(mqttAccount ->{
            MqttAccountInfoWithPass account = MqttAccountInfoWithPass.builder()
                .mqttUsername(mqttAccount.getMqttUsername())
                .mqttPassword(mqttAccount.getMqttPassword())
                .clientId(mqttAccount.getClientId())
                .build();
            return Response.ok(account).build();
        });
        
    }

    public Uni<Response> authenticate(MqttAuthRequest request) {
        log.info("Processing MQTT authentication request for username: " + request.getUsername());
        
        return authenticationUseCase.authenticate(
                request.getUsername(),
                request.getPassword(),
                request.getClientId()
            )
            .onItem().transform(isAuthenticated -> {
                MqttResponse response = isAuthenticated ? 
                    MqttResponse.allow() : MqttResponse.deny();
                
                log.info("Authentication result for username " + request.getUsername() + ": " + 
                        (isAuthenticated ? "ALLOWED" : "DENIED"));
                
                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Authentication service error", throwable);
                return Response.ok(MqttResponse.deny()).build();
            });
    }

    public Uni<Response> authorize(MqttAclRequest request) {
        log.info("Processing MQTT authorization request for username: " + request.getUsername() + 
                ", topic: " + request.getTopic() + ", action: " + request.getAction());
        
        // Use default QoS level 0 for all operations
        int qosLevel = 0;
        
        return authorizationUseCase.authorize(
                request.getUsername(),
                request.getClientId(),
                request.getTopic(),
                request.getAction(),
                request.getQos() != null ? request.getQos() : qosLevel
            )
            .onItem().transform(isAuthorized -> {
                MqttResponse response = isAuthorized ? 
                    MqttResponse.allow() : MqttResponse.deny();
                
                log.info("Authorization result for username " + request.getUsername() + 
                        ", topic " + request.getTopic() + ": " + 
                        (isAuthorized ? "ALLOWED" : "DENIED"));
                
                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Authorization service error", throwable);
                return Response.ok(MqttResponse.deny()).build();
            });
    }


    public Uni<Response> getDeviceInfoByClientId(String clientId) {
        return getDeviceDetailsByClientIdUseCase.execute(clientId).onItem().transform(deviceInfo -> {
            return Response.ok(deviceInfo).build();
        });
    }

    public Uni<Response> getMqttUsernameByClientId(String clientId){
        return getMqttUsernameByClientIdUseCase.execute(clientId).onItem().transform(mqttUsername ->{
            return Response.ok(
                MqttUsernameRes.builder()
                    .mqttUsername(mqttUsername)
                    .build()
            ).build();
        });
    }


    /**
     * Changes the password for a device identified by device UUID
     * 
     * @param deviceUuid The device UUID to change password for
     * @return Response containing the new password (unhashed)
     */
    public Uni<Response> changePassword(String deviceUuid) {
        log.info("Processing change password request for deviceUuid: " + deviceUuid);
        
        return changePasswordUseCase.execute(deviceUuid)
            .onItem().transform(newPassword -> {
                ChangePasswordResponse response = ChangePasswordResponse.builder()
                    .clientId(deviceUuid) // Can be changed to deviceUuid field
                    .newPassword(newPassword)
                    .message("Password changed successfully")
                    .build();
                
                log.info("Password change completed successfully for deviceUuid: " + deviceUuid);
                return Response.ok(response).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to change password for deviceUuid: " + deviceUuid, throwable);
                return Response.status(Response.Status.NOT_FOUND)
                    .entity(Map.of("error", throwable.getMessage()))
                    .build();
            });
    }
}
