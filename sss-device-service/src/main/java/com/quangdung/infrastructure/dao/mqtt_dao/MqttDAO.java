package com.quangdung.infrastructure.dao.mqtt_dao;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.quangdung.infrastructure.entity.mqtt.CreateMqttAccountRequest;
import com.quangdung.infrastructure.entity.mqtt.CreateMqttAccountResponse;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

@Path("/api/v1/mqtt")
@RegisterRestClient(configKey="mqtt-api") 
public interface MqttDAO {

    @POST
    @Path("/create_account")
    Uni<CreateMqttAccountResponse> createMqttAccount(CreateMqttAccountRequest request);
    
}
