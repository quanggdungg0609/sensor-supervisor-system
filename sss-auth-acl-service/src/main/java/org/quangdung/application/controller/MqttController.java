package org.quangdung.application.controller;

import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

import org.jboss.logging.Logger;
import org.quangdung.application.dto.request.MqttAclRequest;
import org.quangdung.application.dto.request.MqttAuthRequest;
import org.quangdung.application.dto.request.MqttCreateAccountRequest;
import org.quangdung.application.service.MqttService;
import org.quangdung.core.metric.MetricService;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.validation.constraints.NotNull;

@Path("/api/v1/mqtt")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class MqttController {
    private final Logger log;
    private final MqttService mqttService;
    private final MetricService metricService;

    @Inject
    public MqttController(
        Logger log,
        MqttService mqttService,
        MetricService metricService
    ){
        this.log = log;
        this.mqttService = mqttService;
        this.metricService = metricService;
    }

    /**
     * Handles MQTT client authentication requests from EMQX HTTP Auth
     * @param request The authentication request containing username and clientId
     * @return Response with authentication result for EMQX
     */
    @POST
    @Path("/auth")
    @WithSession
    public Uni<Response> authentication(
            @NotNull(message = "Request body is required") @Valid MqttAuthRequest request
    ) {
        log.info("Received authentication request for username: " + request.getUsername() + 
                ", clientId: " + request.getClientId());
        
        return metricService.timeOperation("mqtt.controller.auth", () -> {
            return mqttService.authenticate(request)
                .onItem().invoke(response -> {
                    log.info("Authentication response: " + response.getEntity());
                    
                    // Record counter based on response status
                    int statusCode = response.getStatus();
                    String result = (statusCode == 200) ? "success" : "failure";
                    metricService.incrementCounter("mqtt.controller.auth.requests", 
                        "result=" + result, "status_code=" + statusCode);
                })
                .onFailure().invoke(throwable -> {
                    log.error("Authentication failed", throwable);
                    
                    // Record error counter
                    metricService.incrementCounter("mqtt.controller.auth.errors", 
                        "error_type=" + throwable.getClass().getSimpleName());
                });
        }, "endpoint=/auth", "method=POST");
    }

    /**
     * Handles MQTT client authorization requests from EMQX HTTP ACL
     * @param request The authorization request containing topic and action information
     * @return Response with authorization result for EMQX
     */
    @POST
    @Path("/acl")
    @WithSession
    public Uni<Response> authorization(
            @NotNull(message = "Request body is required") @Valid MqttAclRequest request
    ) {
        log.info("Received authorization request for username: " + request.getUsername() + 
                ", topic: " + request.getTopic() + ", action: " + request.getAction());
        
        return metricService.timeOperation("mqtt.controller.acl", () -> {
            return mqttService.authorize(request)
                .onItem().invoke(response -> {
                    log.info("Authorization response: " + response.getEntity());
                    
                    // Record counter based on response status and action type
                    int statusCode = response.getStatus();
                    String result = (statusCode == 200) ? "success" : "failure";
                    metricService.incrementCounter("mqtt.controller.acl.requests", 
                        "result=" + result, 
                        "status_code=" + statusCode,
                        "action=" + request.getAction());
                })
                .onFailure().invoke(throwable -> {
                    log.error("Authorization failed", throwable);
                    
                    // Record error counter
                    metricService.incrementCounter("mqtt.controller.acl.errors", 
                        "error_type=" + throwable.getClass().getSimpleName(),
                        "action=" + request.getAction());
                });
        }, "endpoint=/acl", "method=POST");
    }

    /**
     * Handles MQTT account creation requests
     * @param request The account creation request containing MQTT account details
     * @return Response with creation result
     */
    @POST
    @Path("/create_account")
    @WithTransaction
    public Uni<Response> createAccount(@NotNull(message = "Request body is required") @Valid MqttCreateAccountRequest request){
        log.info("Received MQTT account creation request for username: " + request.getMqttUsername());
        
        return metricService.timeOperation("mqtt.controller.create_account", () -> {
            return mqttService.createNewAccount(request)
                .onItem().invoke(response -> {
                    // Record counter based on response status
                    int statusCode = response.getStatus();
                    String result = (statusCode >= 200 && statusCode < 300) ? "success" : "failure";
                    metricService.incrementCounter("mqtt.controller.create_account.requests", 
                        "result=" + result, "status_code=" + statusCode);
                })
                .onFailure().invoke(throwable -> {
                    log.error("Account creation failed", throwable);
                    
                    // Record error counter
                    metricService.incrementCounter("mqtt.controller.create_account.errors", 
                        "error_type=" + throwable.getClass().getSimpleName());
                });
        }, "endpoint=/create_account", "method=POST");
    }


    @GET
    @Path("/device-info/{clientId}")
    @WithSession
    public Uni<Response> getDeviceInfoByClientId(@PathParam("clientId") String clientId){
        log.info("Received request to get device info for clientId: " + clientId);
        return mqttService.getDeviceInfoByClientId(clientId);
    }
}
