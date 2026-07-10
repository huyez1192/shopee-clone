package com.example.productcrudapi.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class PageResponse<T> {
    private List<T> content;

    private int pageNumber;

    private int pageSize;

    private long totalElements;

    private int totalPages;

    private boolean last;
}
