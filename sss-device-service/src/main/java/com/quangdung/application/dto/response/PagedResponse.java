package com.quangdung.application.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Generic paged response wrapper for API responses with pagination
 * @param <T> Type of data being paginated
 */
@Data
@Builder
public class PagedResponse<T> {
    @JsonProperty("data")
    private List<T> data;
    
    @JsonProperty("page")
    private int page;
    
    @JsonProperty("size")
    private int size;
    
    @JsonProperty("total_elements")
    private long totalElements;
    
    @JsonProperty("total_pages")
    private int totalPages;
    
    @JsonProperty("first")
    private boolean first;
    
    @JsonProperty("last")
    private boolean last;
    
    @JsonProperty("has_next")
    private boolean hasNext;
    
    @JsonProperty("has_previous")
    private boolean hasPrevious;
}