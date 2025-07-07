package org.quangdung.application.controller;

import jakarta.ws.rs.Path;

import org.jboss.logging.Logger;
import org.quangdung.application.dto.request.MqttAclRequest;
import org.quangdung.application.dto.request.MqttAuthRequest;
import org.quangdung.application.dto.request.MqttCreateAccountRequest;
import org.quangdung.application.service.MqttService;

import io.quarkus.hibernate.reactive.panache.common.WithSession;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
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



    @Inject
    public MqttController(
        Logger log,
        MqttService mqttService
    ){
        this.log = log;
        this.mqttService = mqttService;
    }

    @POST
    @Path("/auth")
    @WithSession
    public Uni<Response> authentication(
            @NotNull(message = "Request body is required") @Valid MqttAuthRequest request
    ) {
        log.info("Received authentication request for username: " + request.getUsername() + 
                ", clientId: " + request.getClientId());
        return mqttService.authenticate(request)
            .onItem().invoke(response -> {
                log.info("Authentication response: " + response.getEntity());
            });
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
        return mqttService.authorize(request)
            .onItem().invoke(response -> {
                log.info("Authorization response: " + response.getEntity());
            });
    }

    @POST
    @Path("/create_account")
    @WithTransaction
    public Uni<Response> createAccount(@NotNull(message = "Request body is required") @Valid MqttCreateAccountRequest request){
        log.info("Received MQTT account creation request for username: " + request.getMqttUsername());
        return mqttService.createNewAccount(request);
    }
}
