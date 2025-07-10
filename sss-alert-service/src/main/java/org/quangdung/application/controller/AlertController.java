package org.quangdung.application.controller;

import org.jboss.logging.Logger;
import org.quangdung.application.service.AlertService;
import org.quangdung.infrastructure.component.mail.MailComponent;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.inject.Inject;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("api/v1/alert")
public class AlertController {
    @Inject
    private Logger log;

    @Inject
    private AlertService alertService;





    @POST
    @Path("/hook")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Uni<Response> createAlert(String jsonBody){
          try {
            return alertService.processAlert(jsonBody);
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
}
