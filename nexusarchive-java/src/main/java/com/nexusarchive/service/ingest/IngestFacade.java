// Input: Spring Framework, Lombok
// Output: IngestFacade 类
// Pos: 服务层 - SIP 接收门面

package com.nexusarchive.service.ingest;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.IngestResponse;
import com.nexusarchive.entity.IngestRequestStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * SIP 接收门面服务
 * <p>
 * 协调各个 SIP 处理模块，执行完整的接收流程
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class IngestFacade {

    private final List<IngestValidator> validators;
    private final IngestFileHandler fileHandler;
    private final IngestEventPublisher eventPublisher;
    private final IngestStatusTracker statusTracker;

    /**
     * 接收 SIP 请求
     *
     * @param sipDto SIP 请求
     * @return 接收响应
     */
    @Transactional(rollbackFor = Exception.class)
    public IngestResponse ingestSip(AccountingSipDto sipDto) {
        String requestId = sipDto.getRequestId();
        log.info("收到 SIP 请求: requestId={}, voucher={}", requestId, sipDto.getHeader().getVoucherNumber());

        try {
            // 1. 业务规则校验
            validateBusinessRules(sipDto);

            // 2. 准备文件流并落地到临时目录
            String tempPath = fileHandler.getTempPath(requestId);
            Map<String, byte[]> fileStreams = fileHandler.prepareTempFiles(sipDto, requestId);

            // 3. 初始化请求状态
            statusTracker.initializeStatus(requestId, "已接收请求，开始处理");

            // 4. 发布事件
            eventPublisher.publishVoucherReceivedEvent(sipDto, tempPath, fileStreams);

            // 5. 立即返回
            return IngestResponse.builder()
                    .requestId(requestId)
                    .status("RECEIVED")
                    .archivalCode(null) // 异步生成，此时为空
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .message("请求已接收，正在后台处理。请通过 /status/" + requestId + " 查询进度。")
                    .build();

        } catch (BusinessException e) {
            throw e;
        } catch (IOException e) {
            log.error("SIP 接收失败", e);
            throw new BusinessException(500, "SIP 接收失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("SIP 接收失败", e);
            throw new BusinessException(500, "SIP 接收失败: " + e.getMessage());
        }
    }

    /**
     * 执行业务规则验证
     *
     * @param sipDto SIP 请求
     */
    private void validateBusinessRules(AccountingSipDto sipDto) {
        // 按优先级顺序执行验证
        validators.stream()
                .sorted((v1, v2) -> Integer.compare(v1.getPriority(), v2.getPriority()))
                .forEach(validator -> {
                    log.debug("执行验证器: {}", validator.getName());
                    validator.validate(sipDto);
                });

        log.info("SIP 业务规则验证通过: requestId={}", sipDto.getRequestId());
    }

    /**
     * 查询请求状态
     *
     * @param requestId 请求 ID
     * @return 状态记录
     */
    public IngestRequestStatus getStatus(String requestId) {
        return statusTracker.getStatus(requestId);
    }
}
