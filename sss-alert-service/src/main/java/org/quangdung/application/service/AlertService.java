package org.quangdung.application.service;

import org.jboss.logging.Logger;
import org.quangdung.domain.usecase.interfaces.ISendMailAlertUseCase;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;

@ApplicationScoped
public class AlertService {
    @Inject
    private Logger log;

    @Inject
    private ISendMailAlertUseCase sendMailAlertUseCase;


    public Uni<Response> processAlert(String jsonBody){
        JsonObject jsonObject = new JsonObject(jsonBody);
        String level = jsonObject.getString("_level");
        String message = jsonObject.getString("_message");
        if (level == null || level.isBlank() || message == null || message.isBlank()) {
            return Uni.createFrom().item(
                Response.notAcceptable(null).build()
            );
        }
        log.info("Sending alert email...");

        return sendMailAlertUseCase.execute(level, message)
            .onItem().transform(v -> Response.ok("{\"status\": \"success\", \"message\": \"Manual alert received and parsed\"}").build())
            .onFailure().recoverWithItem(throwable -> Response.serverError().build());
    }
}
