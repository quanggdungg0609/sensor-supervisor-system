package com.quangdung.infrastructure.dao.mqtt_dao;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.quangdung.infrastructure.entity.mqtt.CreateMqttAccountRequest;
import com.quangdung.infrastructure.entity.mqtt.CreateMqttAccountResponse;
import com.quangdung.infrastructure.entity.mqtt.DeleteMqttAccountResponse;
import com.quangdung.infrastructure.entity.mqtt.GetMqttUsernameResponse;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/v1/mqtt")
@RegisterRestClient(configKey="mqtt-api") 
public interface MqttDAO {

    @POST
    @Path("/create_account")
    Uni<CreateMqttAccountResponse> createMqttAccount(CreateMqttAccountRequest request);

    @GET
    @Path("/mqtt-username/{clientId}")
    Uni<GetMqttUsernameResponse> getMqttUsernameByClientId(@PathParam("clientId") String clientId);
    
    /**
     * Deletes an MQTT account by device UUID
     * 
     * @param deviceUuid The device UUID of the account to delete
     * @return Response indicating success or failure
     */
    @DELETE
    @Path("/delete_by_uuid/{deviceUuid}")
    Uni<DeleteMqttAccountResponse> deleteMqttAccountByDeviceUuid(@PathParam("deviceUuid") String deviceUuid);
}
