package org.quangdung.infrastructure.component.device_rest_client_component;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.quangdung.infrastructure.entity.DeviceInfoEntity;

import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/v1/devices")
@RegisterRestClient(configKey = "device-service-api")
public interface DeviceRestClientComponent {

    @GET
    @Path("/get_device_by_clientid/{clientId}")
    public Uni<DeviceInfoEntity> getDeviceByClientId(@PathParam("clientId") String clientId);
    
    @GET
    @Path("/{deviceUuid}")
    public Uni<DeviceInfoEntity> getDeviceByUuid(@PathParam("deviceUuid") String deviceUuid);
}
