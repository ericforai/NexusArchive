package com.nexusarchive.integration.erp.dto;

import lombok.Data;
import java.util.List;

@Data
public class ErpPageResponse<T> {
    private List<T> items;
    private long total;
    private int pageIndex;
    private int pageSize;
    
    public static <T> ErpPageResponse<T> of(List<T> items, long total, int pageIndex, int pageSize) {
        ErpPageResponse<T> response = new ErpPageResponse<>();
        response.setItems(items);
        response.setTotal(total);
        response.setPageIndex(pageIndex);
        response.setPageSize(pageSize);
        return response;
    }
}
