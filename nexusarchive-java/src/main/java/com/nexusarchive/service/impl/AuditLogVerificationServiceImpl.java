// Input: AuditLogVerificationService、SysAuditLogMapper、SM3Utils
// Output: AuditLogVerificationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.ChainVerificationResult;
import com.nexusarchive.dto.VerificationResult;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import com.nexusarchive.service.AuditLogVerificationService;
import com.nexusarchive.util.SM3Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * 审计日志验真服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditLogVerificationServiceImpl implements AuditLogVerificationService {

    private final SysAuditLogMapper auditLogMapper;
    private final SM3Utils sm3Utils;

    @Value("${audit.log.hmac-key:}")
    private String auditLogHmacKey;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public VerificationResult verifySingleLog(String logId) {
        SysAuditLog log = auditLogMapper.selectById(logId);
        if (log == null) {
            return VerificationResult.invalid(
                    logId,
                    VerificationResult.ISSUE_TYPE_MISSING_LOG,
                    "审计日志不存在: " + logId
            );
        }

        VerificationResult hashResult = verifyHash(log);
        if (!hashResult.isValid()) {
            return hashResult;
        }

        SysAuditLog previousLog = resolvePreviousLog(log);
        if (previousLog != null && !Objects.equals(previousLog.getLogHash(), log.getPrevLogHash())) {
            return VerificationResult.invalid(
                    logId,
                    VerificationResult.ISSUE_TYPE_BROKEN_CHAIN,
                    "与前一条日志的哈希关联不匹配",
                    previousLog.getLogHash(),
                    log.getPrevLogHash()
            );
        }

        return VerificationResult.valid(logId, log.getLogHash());
    }

    @Override
    public ChainVerificationResult verifyChain(LocalDate startDate, LocalDate endDate, String fondsNo) {
        List<SysAuditLog> logs = auditLogMapper.findByDateRangeAndFondsNo(startDate, endDate, fondsNo);
        return evaluateChain(logs, null);
    }

    @Override
    public ChainVerificationResult verifyChainByLogIds(List<String> logIds) {
        if (logIds == null || logIds.isEmpty()) {
            return emptyResult();
        }

        List<SysAuditLog> logs = auditLogMapper.findByIdsInOrder(logIds);
        return evaluateChain(logs, new LinkedHashSet<>(logIds));
    }

    private ChainVerificationResult evaluateChain(List<SysAuditLog> logs, Set<String> requestedLogIds) {
        if (logs == null || logs.isEmpty()) {
            if (requestedLogIds == null || requestedLogIds.isEmpty()) {
                return emptyResult();
            }

            List<VerificationResult> missingResults = new ArrayList<>();
            for (String requestedLogId : requestedLogIds) {
                missingResults.add(VerificationResult.invalid(
                        requestedLogId,
                        VerificationResult.ISSUE_TYPE_MISSING_LOG,
                        "审计日志不存在: " + requestedLogId
                ));
            }
            return buildResult(requestedLogIds.size(), missingResults);
        }

        Map<String, VerificationResult> invalidResults = new LinkedHashMap<>();
        Set<String> foundLogIds = new LinkedHashSet<>();
        SysAuditLog previous = null;

        for (SysAuditLog log : logs) {
            foundLogIds.add(log.getId());

            VerificationResult hashResult = verifyHash(log);
            if (!hashResult.isValid()) {
                invalidResults.put(log.getId(), hashResult);
                previous = log;
                continue;
            }

            if (previous != null && !Objects.equals(previous.getLogHash(), log.getPrevLogHash())) {
                invalidResults.put(log.getId(), VerificationResult.invalid(
                        log.getId(),
                        VerificationResult.ISSUE_TYPE_BROKEN_CHAIN,
                        "与前一条日志的哈希关联不匹配",
                        previous.getLogHash(),
                        log.getPrevLogHash()
                ));
            }

            previous = log;
        }

        if (requestedLogIds != null && !requestedLogIds.isEmpty()) {
            for (String requestedLogId : requestedLogIds) {
                if (!foundLogIds.contains(requestedLogId)) {
                    invalidResults.put(requestedLogId, VerificationResult.invalid(
                            requestedLogId,
                            VerificationResult.ISSUE_TYPE_MISSING_LOG,
                            "审计日志不存在: " + requestedLogId
                    ));
                }
            }
        }

        int totalLogs = requestedLogIds != null && !requestedLogIds.isEmpty() ? requestedLogIds.size() : logs.size();
        return buildResult(totalLogs, new ArrayList<>(invalidResults.values()));
    }

    private ChainVerificationResult buildResult(int totalLogs, List<VerificationResult> invalidResults) {
        int missingLogs = 0;
        int brokenChainLogs = 0;
        int tamperedLogs = 0;

        for (VerificationResult invalidResult : invalidResults) {
            switch (invalidResult.getIssueType()) {
                case VerificationResult.ISSUE_TYPE_MISSING_LOG -> missingLogs++;
                case VerificationResult.ISSUE_TYPE_BROKEN_CHAIN -> brokenChainLogs++;
                case VerificationResult.ISSUE_TYPE_HASH_MISMATCH -> tamperedLogs++;
                default -> {
                }
            }
        }

        int invalidCount = invalidResults.size();
        int validLogs = Math.max(totalLogs - invalidCount, 0);

        return ChainVerificationResult.builder()
                .chainIntact(invalidCount == 0)
                .totalLogs(totalLogs)
                .validLogs(validLogs)
                .invalidLogs(invalidCount)
                .missingLogs(missingLogs)
                .brokenChainLogs(brokenChainLogs)
                .tamperedLogs(tamperedLogs)
                .invalidResults(invalidResults)
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    private ChainVerificationResult emptyResult() {
        return ChainVerificationResult.builder()
                .chainIntact(true)
                .totalLogs(0)
                .validLogs(0)
                .invalidLogs(0)
                .missingLogs(0)
                .brokenChainLogs(0)
                .tamperedLogs(0)
                .invalidResults(new ArrayList<>())
                .verifiedAt(LocalDateTime.now())
                .build();
    }

    private VerificationResult verifyHash(SysAuditLog log) {
        String recalculatedHash = calculateLogHash(log);
        if (!Objects.equals(log.getLogHash(), recalculatedHash)) {
            return VerificationResult.invalid(
                    log.getId(),
                    VerificationResult.ISSUE_TYPE_HASH_MISMATCH,
                    "哈希值不匹配",
                    log.getLogHash(),
                    recalculatedHash
            );
        }
        return VerificationResult.valid(log.getId(), log.getLogHash());
    }

    private String calculateLogHash(SysAuditLog log) {
        String createdTimeStr = log.getCreatedTime() != null
                ? log.getCreatedTime().format(TIME_FORMATTER)
                : null;

        String payload = buildHashPayload(
                log.getUserId(),
                log.getAction(),
                log.getObjectDigest(),
                createdTimeStr,
                log.getPrevLogHash()
        );
        return sm3Utils.hmac(auditLogHmacKey, payload);
    }

    private String buildHashPayload(String userId, String action, String objectDigest,
                                    String createdTime, String prevLogHash) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId != null ? userId : "").append("|");
        sb.append(action != null ? action : "").append("|");
        sb.append(objectDigest != null ? objectDigest : "").append("|");
        sb.append(createdTime != null ? createdTime : "").append("|");
        sb.append(prevLogHash != null ? prevLogHash : "");
        return sb.toString();
    }

    private SysAuditLog resolvePreviousLog(SysAuditLog log) {
        LambdaQueryWrapper<SysAuditLog> queryWrapper = new LambdaQueryWrapper<SysAuditLog>()
                .lt(SysAuditLog::getCreatedTime, log.getCreatedTime())
                .orderByDesc(SysAuditLog::getCreatedTime)
                .orderByDesc(SysAuditLog::getId)
                .last("LIMIT 1");
        return auditLogMapper.selectOne(queryWrapper);
    }
}
