package org.quangdung.infrastructure.dao;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.quangdung.infrastructure.entity.DeviceDetailsResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.smallrye.mutiny.Uni;

@Path("/api/v1/devices")
@RegisterRestClient(configKey = "device-service-api") 
@Produces(MediaType.APPLICATION_JSON)
public interface DeviceDAO {
    @GET
    @Path("/{deviceUuid}")
    Uni<DeviceDetailsResponse> getDeviceByUuid(@PathParam("deviceUuid") String deviceUuid);
}
