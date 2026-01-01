// Input: Spring Framework, Lombok
// Output: IngestStatusTracker 类
// Pos: 服务层 - SIP 状态跟踪器

package com.nexusarchive.service.ingest;

import com.nexusarchive.entity.IngestRequestStatus;
import com.nexusarchive.mapper.IngestRequestStatusMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * SIP 状态跟踪器
 * <p>
 * 负责记录和更新 SIP 请求的状态
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestStatusTracker {

    private final IngestRequestStatusMapper ingestRequestStatusMapper;

    /**
     * 初始化请求状态
     *
     * @param requestId 请求 ID
     * @param message 状态消息
     * @return 创建的状态记录
     */
    public IngestRequestStatus initializeStatus(String requestId, String message) {
        IngestRequestStatus status = IngestRequestStatus.builder()
                .requestId(requestId)
                .status("RECEIVED")
                .message(message)
                .build();
        ingestRequestStatusMapper.insert(status);
        log.info("初始化 SIP 请求状态: requestId={}, status=RECEIVED", requestId);
        return status;
    }

    /**
     * 更新请求状态
     *
     * @param requestId 请求 ID
     * @param status 新状态
     * @param message 状态消息
     */
    public void updateStatus(String requestId, String status, String message) {
        IngestRequestStatus existing = ingestRequestStatusMapper.selectById(requestId);
        if (existing != null) {
            existing.setStatus(status);
            existing.setMessage(message);
            ingestRequestStatusMapper.updateById(existing);
            log.info("更新 SIP 请求状态: requestId={}, status={}", requestId, status);
        }
    }

    /**
     * 获取请求状态
     *
     * @param requestId 请求 ID
     * @return 状态记录
     */
    public IngestRequestStatus getStatus(String requestId) {
        return ingestRequestStatusMapper.selectById(requestId);
    }
}
