package org.quangdung.application.service;

import org.jboss.logging.Logger;
import org.quangdung.application.dto.AddThresholdRequestDTO;
import org.quangdung.domain.model.ThresholdModel;
import org.quangdung.domain.usecase.interfaces.IAddAlertEmailUseCase;
import org.quangdung.domain.usecase.interfaces.IDeleteAlertEmailUseCase;
import org.quangdung.domain.usecase.interfaces.IGetAlertEmailsUseCase;
import org.quangdung.domain.usecase.interfaces.IGetAllThresholdsUseCase;
import org.quangdung.domain.usecase.interfaces.IGetDeviceByUuidUseCase;
import org.quangdung.domain.usecase.interfaces.IGetTelemetryValueNamesUseCase;
import org.quangdung.domain.usecase.interfaces.IGetThresholdUseCase;
import org.quangdung.domain.usecase.interfaces.ISaveThresholdUseCase;
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

    @Inject
    private ISaveThresholdUseCase saveThresholdUseCase;

    @Inject
    private IGetAllThresholdsUseCase getAllThresholdsUseCase;

    @Inject
    private IGetDeviceByUuidUseCase getDeviceByUuidUseCase;
    
    @Inject
    private IGetThresholdUseCase getThresholdUseCase;

    @Inject
    private IAddAlertEmailUseCase addAlertEmailUseCase;
    
    @Inject
    private IDeleteAlertEmailUseCase deleteAlertEmailUseCase;
    
    @Inject
    private IGetAlertEmailsUseCase getAlertEmailsUseCase;

    @Inject
    private IGetTelemetryValueNamesUseCase getTelemetryValueNamesUseCase;

    public Uni<Response> addTheshold(String deviceUuid, AddThresholdRequestDTO dto){
        ThresholdModel thresholdModel = ThresholdModel.builder()
            .thresholdLevel(
                switch(dto.getThresholdLevel()){
                    case "CRITICAL" -> ThresholdModel.ThresholdLevel.CRITICAL;
                    case "WARNING" -> ThresholdModel.ThresholdLevel.WARNING;
                    default -> throw new IllegalArgumentException("Invalid threshold level");
                }
            )
            .thresholdType(
                switch(dto.getThresholdType()){
                    case "UPPER" -> ThresholdModel.ThresholdType.UPPER;
                    case "LOWER" -> ThresholdModel.ThresholdType.LOWER;
                    default -> throw new IllegalArgumentException("Invalid threshold type");
                }
            )
            .message(dto.getMessage())
            .threshold(dto.getThreshold())
            .build();
        return getDeviceByUuidUseCase.execute(deviceUuid).onItem().transformToUni(deviceInfo ->
                saveThresholdUseCase.execute(deviceInfo.getClientId(), thresholdModel)
                    .onItem().transform(
                        v -> Response.ok("{\"status\": \"success\", \"message\": \"Threshold added\"}").build()
                    )
                    .onFailure().recoverWithItem(
                        throwable -> Response.serverError().entity(throwable.getMessage()).build()
                    )
        );
    }

    /**
     * Retrieves all threshold settings for a device
     * 
     * @param deviceUuid The device UUID
     * @return Uni<Response> containing all threshold settings in JSON format
     */
    public Uni<Response> getAllThreshold(String deviceUuid){
        return getDeviceByUuidUseCase.execute(deviceUuid)
            .onItem().transformToUni(
                deviceInfo -> getAllThresholdsUseCase.execute(deviceInfo.getClientId()))
                    .onItem().transform(thresholds -> Response.ok(
                        JsonObject.mapFrom(thresholds)
                    ).build())
                    .onFailure().recoverWithItem(throwable -> {
                        log.errorf(throwable, "Failed to retrieve thresholds for deviceUuid: %s", deviceUuid);
                        return Response.serverError().entity(
                            new JsonObject()
                                .put("status", "error")
                                .put("message", "Failed to retrieve thresholds: " + throwable.getMessage())
                                .encode()
                        ).build();
            }).onFailure().recoverWithItem(throwable -> {
                    log.errorf(throwable, "Failed to retrieve threshold for deviceUuid: %s", deviceUuid);
                    return Response.serverError().entity(
                        new JsonObject()
                            .put("status", "error")
                            .put("message", "Failed to retrieve threshold: " + throwable.getMessage())
                            .encode()
                    ).build();
                });
    }

    /**
     * Retrieves threshold settings for a device with specific level
     * 
     * @param deviceUuid The device UUID
     * @param level The threshold level (CRITICAL or WARNING)
     * @return Uni<Response> containing the threshold settings in JSON format
     */
    public Uni<Response> getThreshold(String deviceUuid, String level, String type){
        ThresholdModel.ThresholdLevel thresholdLevel;
        try {
            thresholdLevel = ThresholdModel.ThresholdLevel.valueOf(level.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid threshold level. Valid values are: CRITICAL, WARNING")
                        .encode()
                ).build()
            );
        }

        ThresholdModel.ThresholdType thresholdType;
        try {
            thresholdType = ThresholdModel.ThresholdType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Uni.createFrom().item(
                Response.status(Response.Status.BAD_REQUEST).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Invalid threshold type. Valid values are: UPPER, LOWER")
                        .encode()
                ).build()
            );
        }
        
        return getDeviceByUuidUseCase.execute(deviceUuid)
            .onItem().transformToUni(deviceInfo -> 
                getThresholdUseCase.execute(
                    deviceInfo.getClientId(), 
                    thresholdLevel,
                    thresholdType   
                ).onItem().transform(threshold -> {
                    if (threshold == null) {
                        return Response.status(Response.Status.NOT_FOUND).entity(
                            new JsonObject()
                                .put("status", "error")
                                .put("message", "Threshold not found")
                                .encode()
                        ).build();
                    }
                    return Response.ok(JsonObject.mapFrom(threshold)).build();
                })
                .onFailure().recoverWithItem(throwable -> {
                    log.errorf(throwable, "Failed to retrieve %s threshold for deviceUuid: %s", level, deviceUuid);
                    return Response.serverError().entity(
                        new JsonObject()
                            .put("status", "error")
                            .put("message", "Failed to retrieve threshold: " + throwable.getMessage())
                            .encode()
                    ).build();
                })
            )
            .onFailure().recoverWithItem(throwable -> {
                log.errorf(throwable, "Failed to get device info for deviceUuid: %s", deviceUuid);
                if (throwable.getMessage() != null && throwable.getMessage().contains("not found")) {
                    return Response.status(Response.Status.NOT_FOUND).entity(
                        new JsonObject()
                            .put("status", "error")
                            .put("message", "Device not found: " + deviceUuid)
                            .encode()
                    ).build();
                }
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Internal server error: " + throwable.getMessage())
                        .encode()
                ).build();
            });
    }

    public Uni<Response> deleteThreshold(String deviceUuid, String level, String type){
        return Uni.createFrom().item(null);
    }

    /**
     * Adds an email address to the alert notification list
     * 
     * @param email The email address to add
     * @return Uni<Response> indicating success or failure
     */
    public Uni<Response> addAlertEmail(String email) {
        // First check if the email already exists
        return getAlertEmailsUseCase.execute()
            .onItem().transformToUni(existingEmails -> {
                // If email already exists, return conflict response
                if (existingEmails.contains(email)) {
                    return Uni.createFrom().item(
                        Response.status(Response.Status.CONFLICT).entity(
                            new JsonObject()
                                .put("status", "error")
                                .put("message", "Email address already exists")
                                .encode()
                        ).build()
                    );
                }
                
                // If email doesn't exist, proceed with adding it
                return addAlertEmailUseCase.execute(email)
                    .onItem().transform(success -> {
                        if (Boolean.TRUE.equals(success)) {
                            return Response.ok(
                                new JsonObject()
                                    .put("status", "success")
                                    .put("message", "Email address added successfully")
                                    .encode()
                            ).build();
                        } else {
                            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                                new JsonObject()
                                    .put("status", "error")
                                    .put("message", "Failed to add email address")
                                    .encode()
                            ).build();
                        }
                    });
            })
            .onFailure().recoverWithItem(throwable -> {
                log.errorf(throwable, "Failed to add email address: %s", email);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Failed to add email address: " + throwable.getMessage())
                        .encode()
                ).build();
            });
    }

    /**
     * Deletes an email address from the alert notification list
     * 
     * @param email The email address to delete
     * @return Uni<Response> indicating success or failure
     */
    public Uni<Response> deleteAlertEmail(String email) {
        return deleteAlertEmailUseCase.execute(email)
            .onItem().transform(count -> {
                if (count > 0) {
                    return Response.ok(
                        new JsonObject()
                            .put("status", "success")
                            .put("message", "Email address deleted successfully")
                            .encode()
                    ).build();
                } else {
                    return Response.status(Response.Status.NOT_FOUND).entity(
                        new JsonObject()
                            .put("status", "error")
                            .put("message", "Email address not found")
                            .encode()
                    ).build();
                }
            })
            .onFailure().recoverWithItem(throwable -> {
                log.errorf(throwable, "Failed to delete email address: %s", email);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Failed to delete email address: " + throwable.getMessage())
                        .encode()
                ).build();
            });
    }

    /**
     * Retrieves all email addresses registered for alerts
     * 
     * @return Uni<Response> containing all registered email addresses
     */
    public Uni<Response> getAlertEmails() {
        return getAlertEmailsUseCase.execute()
            .onItem().transform(emails -> 
                Response.ok(
                    new JsonObject()
                        .put("status", "success")
                        .put("emails", emails.stream().toList())
                        .encode()
                ).build()
            )
            .onFailure().recoverWithItem(throwable -> {
                log.error("Failed to retrieve alert emails", throwable);
                return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(
                    new JsonObject()
                        .put("status", "error")
                        .put("message", "Failed to retrieve alert emails: " + throwable.getMessage())
                        .encode()
                ).build();
            });
    }

    public Uni<Response> getTelemetryValueNames(String deviceUuid) {
        return getTelemetryValueNamesUseCase.execute(deviceUuid)
            .collect().first()
            .onItem().transform(fieldNames -> {
                if (fieldNames == null || fieldNames.isEmpty()) {
                    return Response.status(Response.Status.NOT_FOUND)
                        .entity(new JsonObject().put("message", "No telemetry values found for device: " + deviceUuid))
                        .build();
                }
                return Response.ok(new JsonObject().put("telemetry_values", fieldNames)).build();
            })
            .onFailure().recoverWithItem(throwable -> {
                log.error("Error retrieving telemetry value names: " + throwable.getMessage(), throwable);
                return Response.serverError()
                    .entity(new JsonObject().put("message", "Error retrieving telemetry value names"))
                    .build();
            });
    }
}
