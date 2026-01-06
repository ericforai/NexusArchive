package com.nexusarchive.service.erp;

import com.nexusarchive.dto.SyncTaskDTO;
import com.nexusarchive.dto.SyncTaskStatus;
import com.nexusarchive.entity.ErpScenario;
import com.nexusarchive.mapper.ErpScenarioMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsyncErpSyncService {

    private final ErpSyncService erpSyncService;
    private final ErpScenarioMapper erpScenarioMapper;

    /**
     * In-memory task status store.
     *
     * LIMITATION: Task status is lost on server restart. For production use,
     * consider using Redis or database persistence for fault tolerance.
     */
    private final Map<String, SyncTaskStatus> taskStore = new ConcurrentHashMap<>();

    @Async("erpSyncExecutor")
    public void syncScenarioAsync(String taskId, Long scenarioId, String operatorId, String clientIp) {
        try {
            // Update status to RUNNING
            SyncTaskStatus status = SyncTaskStatus.builder()
                .taskId(taskId)
                .status("RUNNING")
                .startTime(LocalDateTime.now())
                .progress(0.0)
                .build();
            taskStore.put(taskId, status);

            // Execute sync
            erpSyncService.syncScenario(scenarioId, operatorId, clientIp);

            // Update status to SUCCESS
            ErpScenario scenario = erpScenarioMapper.selectById(scenarioId);
            SyncTaskStatus finalStatus = SyncTaskStatus.builder()
                .taskId(taskId)
                .status("SUCCESS".equals(scenario.getLastSyncStatus()) ? "SUCCESS" : "FAIL")
                .endTime(LocalDateTime.now())
                .errorMessage("FAIL".equals(scenario.getLastSyncStatus()) ? scenario.getLastSyncMsg() : null)
                .progress(1.0)
                .build();
            taskStore.put(taskId, finalStatus);

        } catch (Exception e) {
            log.error("Async sync failed for task {}", taskId, e);
            SyncTaskStatus errorStatus = SyncTaskStatus.builder()
                .taskId(taskId)
                .status("FAIL")
                .errorMessage(e.getMessage())
                .endTime(LocalDateTime.now())
                .build();
            taskStore.put(taskId, errorStatus);
        }
    }

    public SyncTaskStatus getTaskStatus(String taskId) {
        return taskStore.get(taskId);
    }

    public SyncTaskDTO submitSyncTask(Long scenarioId) {
        String taskId = "sync-" + scenarioId + "-" + System.currentTimeMillis();

        SyncTaskDTO task = SyncTaskDTO.builder()
            .taskId(taskId)
            .status("SUBMITTED")
            .message("同步任务已提交")
            .build();

        // Add to store immediately to avoid race condition
        SyncTaskStatus initialStatus = SyncTaskStatus.builder()
            .taskId(taskId)
            .status("SUBMITTED")
            .startTime(LocalDateTime.now())
            .build();
        taskStore.put(taskId, initialStatus);

        return task;
    }
}
