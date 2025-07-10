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
            // Bước 1: Giải mã JSON một cách đồng bộ.
            // Nếu bước này lỗi, chúng ta sẽ ack message và dừng lại.
            deviceData = objectMapper.readValue(message.getPayload().encode(), DeviceDataEntity.class);
            log.infof("Received and deserialized message for clientId: %s", deviceData.getClientId());
        } catch (Exception e) {
            log.error("Failed to deserialize message. Acknowledging and discarding.", e);
            message.ack(); // Rất quan trọng: ack các message lỗi để không làm tắc hàng đợi
            return Uni.createFrom().voidItem();
        }

        // Bước 2: Bắt đầu chuỗi xử lý reactive
        return deviceInfoDAO.getDeviceInfo(deviceData.getClientId())
            .onItem().transformToUni(info -> {
                // Khối code này được thực thi khi getDeviceInfo thành công.
                // 'info' là đối tượng DeviceInfoResponse.

                TelemetryDataEntity telemetryData = TelemetryDataEntity.builder()
                        .clientId(deviceData.getClientId())
                        .deviceUuid(info.getDeviceUuid())
                        .deviceName(info.getDeviceName())
                        .mqttUsername(info.getMqttUsername())
                        .timestamp(deviceData.getTimestamp().toInstant(ZoneOffset.UTC)) // Chuyển đổi LocalDateTime -> Instant (UTC)
                        .data(deviceData.getData())
                        .build();

                String line = telemetryData.toLineProtocol("telemetry_data");

                // Trả về Uni từ InfluxDAO. Mutiny sẽ tự động "làm phẳng" nó.
                return influxDAO.createTelemetryDataByLineProtocol(line);
            })
            .onFailure().invoke(failure -> {
                // Khối code này được thực thi nếu getDeviceInfo hoặc bước tiếp theo thất bại.
                log.errorf(failure, "Failed to process message for clientId: %s", deviceData.getClientId());
            })
            .onItemOrFailure().transformToUni((result, failure) -> {
                // Khối code này luôn được thực thi, đảm bảo message luôn được ack.
                message.ack();
                return Uni.createFrom().voidItem();

            });
    }
}
