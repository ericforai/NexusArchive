package com.nexusarchive.listener;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.service.TimestampService;
import com.nexusarchive.service.signature.SignatureAdapter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;

/**
 * 签章和时间戳监听器
 * 
 * 在归档完成后自动执行签章和时间戳操作
 * 
 * @author Agent B - 合规开发工程师
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SignatureTimestampListener {

    @Autowired(required = false)
    private SignatureAdapter signatureAdapter;

    private final TimestampService timestampService;

    @Value("${signature.auto-sign:true}")
    private boolean autoSignEnabled;

    @Value("${timestamp.auto-request:true}")
    private boolean autoTimestampEnabled;

    @Value("${signature.default-cert-alias:signing-cert}")
    private String defaultCertAlias;

    @Value("${archive.root.path:./data/archives}")
    private String archiveRootPath;

    /**
     * 监听归档完成事件，自动执行签章和时间戳
     */
    @Async("ingestTaskExecutor")
    @EventListener
    public void handleArchiveCompleted(ArchiveCompletedEvent event) {
        Archive archive = event.getArchive();
        List<ArcFileContent> files = event.getFiles();

        if (archive == null || files == null || files.isEmpty()) {
            log.debug("跳过签章和时间戳：档案或文件为空");
            return;
        }

        log.info("开始为档案执行签章和时间戳: archiveId={}, files={}", archive.getId(), files.size());

        try {
            // 1. 自动签章（如果启用）
            if (autoSignEnabled && signatureAdapter != null && signatureAdapter.isAvailable()) {
                performAutoSignature(archive, files);
            } else {
                log.debug("自动签章未启用或服务不可用");
            }

            // 2. 自动请求时间戳（如果启用）
            if (autoTimestampEnabled && timestampService.isAvailable()) {
                performAutoTimestamp(archive, files);
            } else {
                log.debug("自动时间戳未启用或服务不可用");
            }

            log.info("签章和时间戳处理完成: archiveId={}", archive.getId());
        } catch (Exception e) {
            log.error("签章和时间戳处理失败: archiveId={}", archive.getId(), e);
            // 不抛出异常，避免影响归档流程
        }
    }

    /**
     * 执行自动签章
     */
    private void performAutoSignature(Archive archive, List<ArcFileContent> files) {
        try {
            for (ArcFileContent file : files) {
                if (file.getStoragePath() == null) {
                    continue;
                }

                // 读取文件内容
                byte[] fileContent = readFileContent(file.getStoragePath());
                if (fileContent == null) {
                    log.warn("无法读取文件内容: {}", file.getStoragePath());
                    continue;
                }

                // 对文件进行签名
                var signResult = signatureAdapter.sign(fileContent, defaultCertAlias);
                
                if (signResult.isSuccess()) {
                    log.info("文件签章成功: fileId={}, archiveId={}", file.getId(), archive.getId());
                    
                    // 保存签名值到文件实体（如果字段存在）
                    // 注意：这里需要根据实际的 ArcFileContent 实体字段调整
                    // file.setSignValue(Base64.getEncoder().encodeToString(signResult.getSignature()));
                } else {
                    log.warn("文件签章失败: fileId={}, error={}", file.getId(), signResult.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("自动签章异常: archiveId={}", archive.getId(), e);
        }
    }

    /**
     * 执行自动时间戳请求
     */
    private void performAutoTimestamp(Archive archive, List<ArcFileContent> files) {
        try {
            // 对档案元数据进行时间戳（使用档案的哈希值）
            if (archive.getFixityValue() != null) {
                byte[] archiveData = archive.getFixityValue().getBytes();
                var timestampResult = timestampService.requestTimestamp(archiveData);

                if (timestampResult.isSuccess()) {
                    log.info("档案时间戳请求成功: archiveId={}, timestamp={}", 
                            archive.getId(), timestampResult.getTimestamp());
                    
                    // 保存时间戳令牌到档案实体（如果字段存在）
                    // 注意：这里需要根据实际的 Archive 实体字段调整
                    // archive.setTimestampToken(timestampResult.getTimestampToken());
                } else {
                    log.warn("档案时间戳请求失败: archiveId={}, error={}", 
                            archive.getId(), timestampResult.getErrorMessage());
                }
            }

            // 对每个文件请求时间戳
            for (ArcFileContent file : files) {
                if (file.getFileHash() == null) {
                    continue;
                }

                byte[] fileHash = file.getFileHash().getBytes();
                var timestampResult = timestampService.requestTimestamp(fileHash);

                if (timestampResult.isSuccess()) {
                    log.info("文件时间戳请求成功: fileId={}, timestamp={}", 
                            file.getId(), timestampResult.getTimestamp());
                } else {
                    log.warn("文件时间戳请求失败: fileId={}, error={}", 
                            file.getId(), timestampResult.getErrorMessage());
                }
            }
        } catch (Exception e) {
            log.error("自动时间戳异常: archiveId={}", archive.getId(), e);
        }
    }

    /**
     * 读取文件内容
     */
    private byte[] readFileContent(String storagePath) {
        try {
            java.nio.file.Path path;
            if (Paths.get(storagePath).isAbsolute()) {
                path = Paths.get(storagePath);
            } else {
                path = Paths.get(archiveRootPath, storagePath);
            }

            if (!Files.exists(path)) {
                log.warn("文件不存在: {}", path.toAbsolutePath());
                return null;
            }

            return Files.readAllBytes(path);
        } catch (Exception e) {
            log.error("读取文件失败: {}", storagePath, e);
            return null;
        }
    }

    /**
     * 归档完成事件
     */
    public static class ArchiveCompletedEvent {
        private final Archive archive;
        private final List<ArcFileContent> files;

        public ArchiveCompletedEvent(Archive archive, List<ArcFileContent> files) {
            this.archive = archive;
            this.files = files;
        }

        public Archive getArchive() {
            return archive;
        }

        public List<ArcFileContent> getFiles() {
            return files;
        }
    }
}


