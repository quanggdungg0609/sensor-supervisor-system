package com.quangdung.domain.usecase.implement;

import com.quangdung.application.dto.response.DeviceInfo;
import com.quangdung.application.dto.response.PagedResponse;
import com.quangdung.domain.repository.IDeviceRepository;
import com.quangdung.domain.usecase.interfaces.IGetAllDevicesUseCase;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.stream.Collectors;

/**
 * Use case for getting all devices with pagination support
 */
@ApplicationScoped
public class GetAllDevicesUseCase implements IGetAllDevicesUseCase {
    
    private final IDeviceRepository deviceRepository;
    private final Logger log;

    @Inject
    public GetAllDevicesUseCase(IDeviceRepository deviceRepository, Logger log) {
        this.deviceRepository = deviceRepository;
        this.log = log;
    }

    /**
     * Execute get all devices with pagination
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Paged response containing device information
     */
    @Override
    public Uni<PagedResponse<DeviceInfo>> execute(int page, int size) {
        log.infof("Getting devices with pagination - page: %d, size: %d", page, size);
        
        // Validate pagination parameters
        if (page < 0) {
            page = 0;
        }
        if (size <= 0 || size > 100) {
            size = 20; // Default size
        }
        
        final int finalPage = page;
        final int finalSize = size;
        
        return Uni.combine().all()
            .unis(
                deviceRepository.getAllDevicesWithPagination(finalPage, finalSize),
                deviceRepository.getTotalDevicesCount()
            )
            .asTuple()
            .onItem().transform(tuple -> {
                var devices = tuple.getItem1();
                var totalElements = tuple.getItem2();
                
                var deviceInfos = devices.stream()
                    .map(device -> DeviceInfo.builder()
                        .deviceUuid(device.getDeviceUuid().toString())
                        .deviceName(device.getDeviceName())
                        .mqttUsername(device.getMqttUsername())
                        .build())
                    .collect(Collectors.toList());
                
                int totalPages = (int) Math.ceil((double) totalElements / finalSize);
                
                return PagedResponse.<DeviceInfo>builder()
                    .data(deviceInfos)
                    .page(finalPage)
                    .size(finalSize)
                    .totalElements(totalElements)
                    .totalPages(totalPages)
                    .first(finalPage == 0)
                    .last(finalPage >= totalPages - 1)
                    .hasNext(finalPage < totalPages - 1)
                    .hasPrevious(finalPage > 0)
                    .build();
            })
            .onFailure().transform(throwable -> {
                log.error("Error in GetAllDevicesUseCase", throwable);
                return new RuntimeException("Failed to get devices with pagination", throwable);
            });
    }
}