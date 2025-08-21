package com.quangdung.domain.usecase.interfaces;

import com.quangdung.application.dto.response.PagedResponse;
import com.quangdung.application.dto.response.DeviceInfo;
import io.smallrye.mutiny.Uni;

public interface IGetAllDevicesUseCase {
    /**
     * Execute get all devices with pagination
     * @param page Page number (0-based)
     * @param size Number of items per page
     * @return Paged response containing device information
     */
    Uni<PagedResponse<DeviceInfo>> execute(int page, int size);
}