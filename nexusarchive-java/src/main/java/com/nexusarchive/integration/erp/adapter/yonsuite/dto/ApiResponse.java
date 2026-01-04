package com.nexusarchive.integration.erp.adapter.yonsuite.dto;

import lombok.Data;
import java.util.List;

@Data
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
}
