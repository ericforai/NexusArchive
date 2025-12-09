package com.nexusarchive.common.result;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 批量操作结果
 * @param <T> 成功项的类型
 */
@Data
public class BatchOperationResult<T> {
    private List<T> successItems = new ArrayList<>();
    private Map<String, String> failures = new HashMap<>();
    
    public void addSuccess(T item) {
        successItems.add(item);
    }
    
    public void addFailure(String id, String reason) {
        failures.put(id, reason);
    }
    
    public boolean hasFailures() {
        return !failures.isEmpty();
    }
    
    public int getSuccessCount() {
        return successItems.size();
    }
    
    public int getFailureCount() {
        return failures.size();
    }
}
