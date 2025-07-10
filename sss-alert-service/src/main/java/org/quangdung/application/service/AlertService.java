package org.quangdung.application.service;

import io.smallrye.mutiny.Uni;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class AlertService {
    @Inject
    private Logger log;


    public Uni<Response> processAlert(String jsonBody){
        JsonObject jsonObject = new JsonObject(jsonBody);
        log.info("Vert.x JsonObject: " + jsonObject.encodePrettily());
        return null;
    }
}
