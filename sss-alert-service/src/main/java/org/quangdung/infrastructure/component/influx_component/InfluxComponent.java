package org.quangdung.infrastructure.component.influx_component;

import java.util.List;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.query.FluxTable;

import io.smallrye.mutiny.Multi;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InfluxComponent {
    @Inject
    private Logger log;

     @Inject
    @ConfigProperty(name = "influxClient.url")
    String url;

    @Inject
    @ConfigProperty(name = "influxClient.token")
    String token;

    @Inject
    @ConfigProperty(name = "influxClient.org")
    String org;

    @Inject
    @ConfigProperty(name = "influxClient.bucketName")
    String bucketName;

    private InfluxDBClient influxDBClient;

    @PostConstruct
    private void init(){
        log.info("Initializing InfluxDB connection");
        log.info(bucketName);
        this.influxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), org, bucketName);
    }


    /**
     * Retrieves all telemetry field names for a specific device
     * 
     * @param deviceUuid The device UUID to retrieve field names for
     * @return Multi<List<String>> A stream of lists containing field names
     */
    public Multi<List<String>> getTelemetryValueNames(String deviceUuid) {
        log.info("Getting telemetry value names for device: " + deviceUuid);
        
        String query = String.format(
            "from(bucket: \"%s\") " +
            "  |> range(start: -30d) " +  // Look back 30 days
            "  |> filter(fn: (r) => r._measurement == \"telemetry_data\") " +
            "  |> filter(fn: (r) => r.deviceUuid == \"%s\") " + // Changed from clientId to deviceUuid
            "  |> group() " +  // Group all data
            "  |> distinct(column: \"_field\") " +  // Get distinct field values
            "  |> yield(name: \"field_names\")",
            bucketName, deviceUuid
        );
        
        log.info("Executing query: " + query);
        
        return Multi.createFrom().emitter(emitter -> {
            try {
                // Execute the query and collect field names
                List<FluxTable> tables = influxDBClient.getQueryApi().query(query);
                
                log.info("Query returned " + tables.size() + " tables");
                
                // Process all records from all tables
                List<String> fieldNames = tables.stream()
                    .flatMap(table -> {
                        log.info("Table has " + table.getRecords().size() + " records");
                        return table.getRecords().stream();
                    })
                    .map(record -> {
                        // Khi sử dụng distinct(), giá trị sẽ nằm trong cột _value
                        Object fieldValue = record.getValueByKey("_value"); 
                        log.info("Found field value: " + (fieldValue != null ? fieldValue.toString() : "null"));
                        return fieldValue != null ? fieldValue.toString() : null;
                    })
                    .filter(fieldName -> fieldName != null) // Filter out null values
                    .distinct()
                    .toList();
                
                // Emit the list of field names
                log.info("Found " + fieldNames.size() + " telemetry fields for device: " + deviceUuid);
                emitter.emit(fieldNames);
                emitter.complete();
            } catch (Exception e) {
                log.error("Error retrieving telemetry field names: " + e.getMessage(), e);
                emitter.fail(e);
            }
        });
    }

    @PreDestroy
    private void close(){
        log.info("Closing InfluxDB connection");
        influxDBClient.close();
    }
}
