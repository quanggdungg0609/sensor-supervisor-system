package com.quangdung.application.controller;


import org.jboss.logging.Logger;

import com.quangdung.application.dto.request.CreateDeviceRequest;
import com.quangdung.application.service.DeviceService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import io.quarkus.hibernate.reactive.panache.common.WithTransaction;
import io.smallrye.mutiny.Uni;


@Path("/api/v1/devices")
@Produces(MediaType.APPLICATION_JSON) 
@Consumes(MediaType.APPLICATION_JSON) 
public class DeviceController {
    @Inject
    private Logger log;
    
    @Inject
    DeviceService deviceService;

    @POST
    @Path("/create_device")
    @WithTransaction
    public Uni<Response> createDevice(
        @NotNull(message ="Request body missing") @Valid CreateDeviceRequest request
    ){
        log.infof("Creating new device {}", request);
        return deviceService.createDevice(request);
    }

}
