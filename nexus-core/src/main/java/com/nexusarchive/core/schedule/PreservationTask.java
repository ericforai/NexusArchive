// Input: None (Scheduled)
// Output: Trigger Service
// Pos: NexusCore schedule
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.core.schedule;

import com.nexusarchive.core.preservation.fixity.FixityCheckService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PreservationTask {

    private final FixityCheckService fixityCheckService;

    /**
     * Daily Integrity Check (Fixity Check)
     * Runs at 02:00 AM every day
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyFixityCheck() {
        log.info("[PreservationTask] Starting daily fixity check...");
        int count = fixityCheckService.performBatchCheck(1000); // Batch size constraint
        log.info("[PreservationTask] Daily fixity check finished. Items checked: {}", count);
    }
}
