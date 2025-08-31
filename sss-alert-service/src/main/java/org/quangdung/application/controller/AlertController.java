package org.quangdung.application.controller;

import org.jboss.logging.Logger;
import org.quangdung.application.dto.AddThresholdRequestDTO;
import org.quangdung.application.dto.EmailAlertRequestDTO;
import org.quangdung.application.service.AlertService;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("api/v1/threshold")

public class AlertController {
    @Inject
    private Logger log;

    @Inject
    private AlertService alertService;


    @POST
    @Path("/add_alert/{deviceUuid}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> addAlert(@PathParam("deviceUuid") String deviceUuid, AddThresholdRequestDTO dto) {
        try {
            return alertService.addTheshold(deviceUuid, dto);
        } catch (Exception e) {
            log.error("Error parsing JSON string: " + e.getMessage(), e);
            // Return a bad request or server error response
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST)
                        .entity("{\"status\": \"error\", \"message\": \"Invalid JSON format: " + e.getMessage() + "\"}")
                        .build()
            );
        }
    }

    @GET
    @Path("/get_all_thresholds/{deviceUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getAllThresholds(@PathParam("deviceUuid") String deviceUuid){
        return alertService.getAllThreshold(deviceUuid);
    }

    @GET
    @Path("/get_threshold/{deviceUuid}/{level}/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getThreshold(@PathParam("deviceUuid") String deviceUuid, @PathParam("level") String level, @PathParam("type") String type){
        return alertService.getThreshold(deviceUuid, level, type);
    }

    @DELETE
    @Path("/delete_threshold/{deviceUuid}/{level}/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> deleteThreshold(@PathParam("deviceUuid") String deviceUuid, @PathParam("level") String level, @PathParam("type") String type){
        return alertService.deleteThreshold(deviceUuid, level, type);
    }


    /**
     * Adds an email address to the alert notification list
     * 
     * @param jsonObject JSON object containing the email address
     * @return Response indicating success or failure
     */
    @POST
    @Path("/add_email_alert")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> addEmailAlert(EmailAlertRequestDTO dto){
        if (dto.getEmail() == null || dto.getEmail().isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Email address is required")
                        .encode()
                ).build()
            );
        }
        
        return alertService.addAlertEmail(dto.getEmail());
    }

    /**
     * Deletes an email address from the alert notification list
     * 
     * @param jsonObject JSON object containing the email address
     * @return Response indicating success or failure
     */
    @DELETE
    @Path("/delete_email_alert")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Uni<Response> deleteEmailAlert(EmailAlertRequestDTO dto){
        String email =dto.getEmail();
        if (email == null || email.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Email address is required")
                        .encode()
                ).build()
            );
        }
        
        return alertService.deleteAlertEmail(email);
    }


    /**
     * Retrieves all email addresses registered for alerts
     * 
     * @return Response containing all registered email addresses
     */
    @GET
    @Path("/get_all_email_alerts")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getAllEmailAlerts(){
        return alertService.getAlertEmails();
    }

    /**
     * Retrieves all telemetry value names for a specific device
     * 
     * @param deviceUuid The device UUID to retrieve telemetry value names for
     * @return Response containing all telemetry value names for the device
     */
    @GET
    @Path("/get_telemetry_values/{deviceUuid}")
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> getTelemetryValueNames(@PathParam("deviceUuid") String deviceUuid) {
        if (deviceUuid == null || deviceUuid.isBlank()) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Device UUID is required")
                        .encode()
                ).build()
            );
        }
        
        return alertService.getTelemetryValueNames(deviceUuid);
    }
}
