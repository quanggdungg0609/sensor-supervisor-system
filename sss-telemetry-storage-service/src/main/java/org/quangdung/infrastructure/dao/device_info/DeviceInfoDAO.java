package org.quangdung.infrastructure.dao.device_info;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;
import org.quangdung.infrastructure.entity.auth_acl_service.DeviceInfoResponse;

import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;

@Path("/api/v1/mqtt/device-info")
@RegisterRestClient(configKey = "device-info-api")
public interface DeviceInfoDAO {
    @GET
    @Path("/{clientId}")
    @CacheResult(cacheName = "device-info-cache")
    Uni<DeviceInfoResponse> getDeviceInfo(@PathParam("clientId") String clientId);
}
