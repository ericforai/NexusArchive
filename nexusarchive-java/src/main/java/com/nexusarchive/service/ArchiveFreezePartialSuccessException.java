package com.nexusarchive.service;

import java.util.List;

public class ArchiveFreezePartialSuccessException extends RuntimeException {

    private final int successCount;
    private final List<String> failedIds;

    public ArchiveFreezePartialSuccessException(String message, int successCount, List<String> failedIds) {
        super(message);
        this.successCount = successCount;
        this.failedIds = failedIds;
    }

    public int getSuccessCount() {
        return successCount;
    }

    public List<String> getFailedIds() {
        return failedIds;
    }
}
