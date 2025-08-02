package org.quangdung.infrastructure.component.rabbitmq;

import java.time.ZoneOffset;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quangdung.infrastructure.dao.device_info.DeviceInfoDAO;
import org.quangdung.infrastructure.dao.influx.InfluxDAO;
import org.quangdung.infrastructure.entity.influx.PowerStatusEntity;
import org.quangdung.infrastructure.entity.influx.TelemetryDataEntity;
import io.smallrye.reactive.messaging.annotations.Merge;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class PowerOutageAlertConsumerComponent {
    @Inject
    private  Logger log;
    @Inject
    private  ObjectMapper objectMapper;

    @Inject
    @RestClient
    private DeviceInfoDAO deviceInfoDAO;

    @Inject
    private InfluxDAO influxDAO;

    @Merge(Merge.Mode.MERGE)
    @Incoming("power-outage-alert-from-rabbitmq")
    public Uni<Void> processPowerOutageAlert(Message<JsonObject> message) {
        PowerStatusEntity powerStatusData;
        
        try {
            powerStatusData = objectMapper.readValue(message.getPayload().encode(), PowerStatusEntity.class);
            log.infof("Received power outage alert for clientId: %s, status: %s", 
                     powerStatusData.getClientId(), powerStatusData.getPowerStatus());
        } catch (Exception e) {
            log.error("Failed to deserialize power outage alert message. Acknowledging and discarding.", e);
            message.ack();
            return Uni.createFrom().voidItem();
        }

        return deviceInfoDAO.getDeviceInfo(powerStatusData.getClientId())
            .onItem().transformToUni(info -> {
                // Create telemetry data with only power_status update
                TelemetryDataEntity telemetryData = TelemetryDataEntity.builder()
                        .clientId(powerStatusData.getClientId())
                        .deviceUuid(info.getDeviceUuid())
                        .deviceName(info.getDeviceName())
                        .mqttUsername(info.getMqttUsername())
                        .timestamp(powerStatusData.getTimestamp().toInstant(ZoneOffset.UTC))
                        .data(java.util.Map.of("power_status", powerStatusData.getPowerStatus()))
                        .build();

                String line = telemetryData.toLineProtocol("telemetry_data");
                return influxDAO.createTelemetryDataByLineProtocol(line);
            })
            .onFailure().invoke(failure -> {
                log.errorf(failure, "Failed to process power outage alert for clientId: %s", 
                          powerStatusData.getClientId());
            })
            .onItemOrFailure().transformToUni((result, failure) -> {
                message.ack();
                return Uni.createFrom().voidItem();
            });
    }

}
