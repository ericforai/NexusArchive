// Input: complexity-history.json 文件读取、Node.js 脚本调用
// Output: 质量监控服务实现
// Pos: Service 层 - 质量监控
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.nexusarchive.dto.quality.*;
import com.nexusarchive.service.QualityMonitorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * 质量监控服务实现
 * 读取 docs/metrics/complexity-history.json 并提供 API 接口
 * 生成快照时调用 node scripts/complexity-snapshot.cjs
 *
 * @author Agent D (基础设施工程师)
 */
@Service
@Slf4j
public class QualityMonitorServiceImpl implements QualityMonitorService {

    private final ObjectMapper objectMapper;
    private final String historyFilePath;
    private final String projectRootDir;

    public QualityMonitorServiceImpl(
            @Value("${quality.metrics.path:docs/metrics/complexity-history.json}") String historyFilePath,
            @Value("${quality.project.root:}") String projectRootDir) {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.registerModule(new JavaTimeModule());
        this.historyFilePath = historyFilePath;

        // 如果未配置，尝试从当前目录或父目录查找项目根目录
        if (projectRootDir == null || projectRootDir.isEmpty()) {
            String userDir = System.getProperty("user.dir");
            // 如果在 nexusarchive-java 目录下，需要回到父目录
            if (userDir.endsWith("nexusarchive-java")) {
                this.projectRootDir = Paths.get(userDir).getParent().toString();
            } else {
                this.projectRootDir = userDir;
            }
        } else {
            this.projectRootDir = projectRootDir;
        }

        log.info("[QualityMonitor] Initialized with history file: {}, project root: {}",
                historyFilePath, this.projectRootDir);
    }

    @Override
    public ComplexityHistoryDto getComplexityHistory() {
        try {
            File file = getHistoryFile();
            if (!file.exists()) {
                log.warn("[QualityMonitor] History file not found, returning empty data");
                return createEmptyHistory();
            }

            return objectMapper.readValue(file, ComplexityHistoryDto.class);
        } catch (IOException e) {
            log.error("[QualityMonitor] Failed to read history file", e);
            return createEmptyHistory();
        }
    }

    @Override
    public ComplexityHistoryDto generateSnapshot() {
        try {
            // 调用 Node.js 脚本生成快照
            executeSnapshotScript();

            // 重新读取更新后的历史
            return getComplexityHistory();
        } catch (Exception e) {
            log.error("[QualityMonitor] Failed to generate snapshot", e);
            throw new RuntimeException("Failed to generate snapshot: " + e.getMessage(), e);
        }
    }

    private File getHistoryFile() {
        // 支持相对路径和绝对路径
        Path path = Paths.get(historyFilePath);
        if (!path.isAbsolute()) {
            path = Paths.get(projectRootDir).resolve(historyFilePath);
        }
        return path.toFile();
    }

    private ComplexityHistoryDto createEmptyHistory() {
        HistoryMetadataDto metadata = HistoryMetadataDto.builder()
                .formatVersion("1.0")
                .createdAt(Instant.now())
                .lastUpdated(Instant.now())
                .build();

        return ComplexityHistoryDto.builder()
                .metadata(metadata)
                .snapshots(new ArrayList<>())
                .build();
    }

    private void executeSnapshotScript() throws IOException, InterruptedException {
        String scriptPath = Paths.get(projectRootDir, "scripts", "complexity-snapshot.cjs").toString();

        File scriptFile = new File(scriptPath);
        if (!scriptFile.exists()) {
            throw new IOException("Snapshot script not found: " + scriptPath);
        }

        log.info("[QualityMonitor] Executing snapshot script: {}", scriptPath);

        ProcessBuilder pb = new ProcessBuilder();
        pb.command("node", scriptPath);
        pb.directory(new File(projectRootDir));
        pb.redirectErrorStream(true);

        Process process = pb.start();

        // 读取输出以便在日志中显示
        StringBuilder output = new StringBuilder();
        try (var reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
                log.info("[QualityMonitor] Script output: {}", line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("[QualityMonitor] Script exited with code: {}, output: {}", exitCode, output);
        } else {
            log.info("[QualityMonitor] Snapshot script completed successfully");
        }
    }
}
