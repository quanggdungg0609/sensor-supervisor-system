package org.quangdung.infrastructure.component.rabbitmq;
import java.time.ZoneOffset;

import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;
import org.quangdung.infrastructure.dao.device_info.DeviceInfoDAO;
import org.quangdung.infrastructure.dao.influx.InfluxDAO;
import org.quangdung.infrastructure.entity.DeviceDataEntity;
import org.quangdung.infrastructure.entity.influx.TelemetryDataEntity;
import io.smallrye.reactive.messaging.annotations.Merge;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.smallrye.mutiny.Uni;
import io.vertx.core.json.JsonObject;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class DataCollectComsumerComponent {
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
    @Incoming("device-data-in-from-rabbitmq") 
    public Uni<Void> processDeviceData(Message<JsonObject> message) { 
         DeviceDataEntity deviceData;
        try {
            deviceData = objectMapper.readValue(message.getPayload().encode(), DeviceDataEntity.class);
            log.infof("Received and deserialized message for clientId: %s", deviceData.getClientId());
        } catch (Exception e) {
            log.error("Failed to deserialize message. Acknowledging and discarding.", e);
            message.ack(); 
            return Uni.createFrom().voidItem();
        }

        return deviceInfoDAO.getDeviceInfo(deviceData.getClientId())
            .onItem().transformToUni(info -> {


                TelemetryDataEntity telemetryData = TelemetryDataEntity.builder()
                        .clientId(deviceData.getClientId())
                        .deviceUuid(info.getDeviceUuid())
                        .deviceName(info.getDeviceName())
                        .mqttUsername(info.getMqttUsername())
                        .timestamp(deviceData.getTimestamp().toInstant(ZoneOffset.UTC))
                        .data(deviceData.getData())
                        .build();

                String line = telemetryData.toLineProtocol("telemetry_data");

                return influxDAO.createTelemetryDataByLineProtocol(line);
            })
            .onFailure().invoke(failure -> {
                log.errorf(failure, "Failed to process message for clientId: %s", deviceData.getClientId());
            })
            .onItemOrFailure().transformToUni((result, failure) -> {
                message.ack();
                return Uni.createFrom().voidItem();

            });
    }
}
