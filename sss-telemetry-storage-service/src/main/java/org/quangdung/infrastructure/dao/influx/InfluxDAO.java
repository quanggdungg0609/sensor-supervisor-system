package org.quangdung.infrastructure.dao.influx;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.quangdung.infrastructure.entity.influx.TelemetryDataEntity;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;

import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class InfluxDAO {
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

    public Uni<Void> createTelemetryDataByLineProtocol(String line){
        return Uni.createFrom().voidItem()
            .runSubscriptionOn(Infrastructure.getDefaultWorkerPool()) 
            .onItem().invoke(() -> {
                log.info("Writing telemetry data to InfluxDB: " +line);
                WriteApiBlocking writeApi = influxDBClient.getWriteApiBlocking();
                writeApi.writeRecord(WritePrecision.NS, line);
            })
            .onFailure().invoke(failure -> {
                // It's good practice to handle potential errors from the DAO
                log.error("Failed to write to InfluxDB", failure);
        });
    }

    @PreDestroy
    private void close(){
        log.info("Closing InfluxDB connection");
        influxDBClient.close();
    }
}
