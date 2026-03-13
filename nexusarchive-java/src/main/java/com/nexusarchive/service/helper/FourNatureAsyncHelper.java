package com.nexusarchive.service.helper;

import com.nexusarchive.dto.sip.report.CheckItem;
import com.nexusarchive.dto.sip.report.FourNatureReport;
import com.nexusarchive.dto.sip.report.OverallStatus;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcSignatureLog;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.mapper.ArcSignatureLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class FourNatureAsyncHelper {

    private final ArcSignatureLogMapper arcSignatureLogMapper;

    public void persistVerificationLog(String archiveId, ArcFileContent file, CheckItem result) {
        if (!supportsSignature(file.getFileType())) return;
        String msg = resolveMsg(result);
        ArcSignatureLog entry = new ArcSignatureLog();
        entry.setArchiveId(archiveId);
        entry.setFileId(file.getId());
        entry.setVerifyResult(resolveRes(result));
        entry.setVerifyTime(LocalDateTime.now());
        entry.setVerifyMessage(msg);
        try { arcSignatureLogMapper.insert(entry); } catch (Exception e) { log.warn("Log failed: {}", e.getMessage()); }
    }

    private boolean supportsSignature(String type) { return "PDF".equalsIgnoreCase(type) || "OFD".equalsIgnoreCase(type); }

    private String resolveMsg(CheckItem r) {
        if (r == null) return "不可用";
        String m = r.getMessage();
        String e = (r.getErrors() != null && !r.getErrors().isEmpty()) ? String.join("; ", r.getErrors()) : null;
        if (m != null && e != null) return m + "; " + e;
        return m != null ? m : (e != null ? e : "为空");
    }

    private String resolveRes(CheckItem r) {
        if (r == null) return "UNKNOWN";
        if (r.getStatus() == OverallStatus.PASS) return "VALID";
        if (r.getStatus() == OverallStatus.FAIL) return "INVALID";
        return "UNKNOWN";
    }

    public void merge(CheckItem target, CheckItem source, List<String> details, String fileName) {
        if (source.getStatus() == OverallStatus.FAIL) {
            target.setStatus(OverallStatus.FAIL);
            target.addError(fileName + ": " + source.getMessage());
        } else if (source.getStatus() == OverallStatus.WARNING) {
            if (target.getStatus() != OverallStatus.FAIL) target.setStatus(OverallStatus.WARNING);
            details.add(fileName + ": " + source.getMessage());
        } else if (source.getMessage() != null && !source.getMessage().isEmpty()) {
            details.add(fileName + ": " + source.getMessage());
        }
    }

    public FourNatureReport buildReport(Archive a, CheckItem auth, CheckItem integ, CheckItem usable, CheckItem safe) {
        OverallStatus status = OverallStatus.PASS;
        if (auth.getStatus() == OverallStatus.FAIL || integ.getStatus() == OverallStatus.FAIL || usable.getStatus() == OverallStatus.FAIL || safe.getStatus() == OverallStatus.FAIL) status = OverallStatus.FAIL;
        else if (auth.getStatus() == OverallStatus.WARNING || integ.getStatus() == OverallStatus.WARNING || usable.getStatus() == OverallStatus.WARNING || safe.getStatus() == OverallStatus.WARNING) status = OverallStatus.WARNING;

        return FourNatureReport.builder()
                .checkId(UUID.randomUUID().toString())
                .checkTime(LocalDateTime.now())
                .archivalCode(a.getArchiveCode())
                .status(status)
                .authenticity(auth).integrity(integ).usability(usable).safety(safe)
                .build();
    }
}
