package com.nexusarchive.service.ofd.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.OfdPreviewResourceResponse;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.OriginalVoucherFile;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.OriginalVoucherFileMapper;
import com.nexusarchive.service.FileStorageService;
import com.nexusarchive.service.ofd.OfdPreviewResourceService;
import com.nexusarchive.service.preview.PreviewFilePathResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

/**
 * OFD 预览资源决策服务实现。
 */
@Service
@RequiredArgsConstructor
public class OfdPreviewResourceServiceImpl implements OfdPreviewResourceService {

    private static final List<String> CONVERTED_EXTENSIONS = List.of("pdf", "png", "jpg", "jpeg");

    private final PreviewFilePathResolver previewFilePathResolver;
    private final ArcFileContentMapper arcFileContentMapper;
    private final OriginalVoucherFileMapper originalVoucherFileMapper;
    private final FileStorageService fileStorageService;

    @Override
    public OfdPreviewResourceResponse resolve(String fileId) {
        PreviewFilePathResolver.ResolvedPreviewFile source = previewFilePathResolver.resolveFileById(fileId);
        if (source == null) {
            throw new IllegalArgumentException("OFD 文件不存在: " + fileId);
        }
        if (!isOfd(source.fileType(), source.fileName())) {
            throw new IllegalArgumentException("仅支持 OFD 文件预览决策: " + fileId);
        }

        OfdPreviewResourceResponse response = new OfdPreviewResourceResponse();
        response.setPreferredMode("liteofd");
        response.setOriginalFileId(fileId);
        response.setOriginalDownloadUrl(buildDownloadUrl(resolveSourceKind(fileId), fileId));
        response.setFileName(source.fileName());

        String sourceKind = resolveSourceKind(fileId);
        Optional<ResolvedConvertedArtifact> converted = "archive".equals(sourceKind)
            ? resolveArchiveConvertedArtifact(fileId, source)
            : resolveOriginalConvertedArtifact(fileId, source);

        converted.ifPresent(artifact -> {
            response.setPreferredMode("converted");
            response.setConvertedFileId(artifact.fileId());
            response.setConvertedMimeType(artifact.mimeType());
            response.setConvertedPreviewUrl(artifact.previewUrl());
        });

        return response;
    }

    private Optional<ResolvedConvertedArtifact> resolveArchiveConvertedArtifact(
        String fileId,
        PreviewFilePathResolver.ResolvedPreviewFile source
    ) {
        ArcFileContent sourceRecord = arcFileContentMapper.selectById(fileId);
        if (sourceRecord == null || !StringUtils.hasText(sourceRecord.getItemId())) {
            return scanSiblingFiles(source, "archive");
        }

        String baseName = getBaseName(source.fileName());
        List<ArcFileContent> siblings = arcFileContentMapper.selectList(
            new LambdaQueryWrapper<ArcFileContent>()
                .eq(ArcFileContent::getItemId, sourceRecord.getItemId())
        );

        return siblings.stream()
            .filter(Objects::nonNull)
            .filter(candidate -> !fileId.equals(candidate.getId()))
            .filter(candidate -> hasSameBaseName(baseName, candidate.getFileName()))
            .filter(candidate -> isConvertedArtifact(candidate.getFileType(), candidate.getFileName()))
            .sorted((left, right) -> compareConvertedPriority(left.getFileName(), right.getFileName()))
            .findFirst()
            .map(candidate -> new ResolvedConvertedArtifact(
                candidate.getId(),
                toMimeType(candidate.getFileType(), candidate.getFileName()),
                "/api/preview?resourceType=file&fileId=" + candidate.getId() + "&mode=stream"
            ))
            .or(() -> scanSiblingFiles(source, "archive"));
    }

    private Optional<ResolvedConvertedArtifact> resolveOriginalConvertedArtifact(
        String fileId,
        PreviewFilePathResolver.ResolvedPreviewFile source
    ) {
        OriginalVoucherFile sourceRecord = originalVoucherFileMapper.selectById(fileId);
        if (sourceRecord == null || !StringUtils.hasText(sourceRecord.getVoucherId())) {
            return scanSiblingFiles(source, "original");
        }

        String baseName = getBaseName(source.fileName());
        return originalVoucherFileMapper.findByVoucherId(sourceRecord.getVoucherId()).stream()
            .filter(Objects::nonNull)
            .filter(candidate -> !fileId.equals(candidate.getId()))
            .filter(candidate -> hasSameBaseName(baseName, candidate.getFileName()))
            .filter(candidate -> isConvertedArtifact(candidate.getFileType(), candidate.getFileName()))
            .sorted((left, right) -> compareConvertedPriority(left.getFileName(), right.getFileName()))
            .findFirst()
            .map(candidate -> new ResolvedConvertedArtifact(
                candidate.getId(),
                toMimeType(candidate.getFileType(), candidate.getFileName()),
                buildDownloadUrl("original", candidate.getId())
            ))
            .or(() -> scanSiblingFiles(source, "original"));
    }

    private Optional<ResolvedConvertedArtifact> scanSiblingFiles(
        PreviewFilePathResolver.ResolvedPreviewFile source,
        String sourceKind
    ) {
        Path sourcePath = fileStorageService.resolvePath(source.storagePath());
        Path parent = sourcePath != null ? sourcePath.getParent() : null;
        if (parent == null) {
            return Optional.empty();
        }

        String baseName = getBaseName(source.fileName());
        for (String ext : CONVERTED_EXTENSIONS) {
            Path sibling = parent.resolve(baseName + "." + ext);
            if (Files.exists(sibling)) {
                return Optional.of(new ResolvedConvertedArtifact(
                    null,
                    toMimeType(ext, sibling.getFileName().toString()),
                    null
                ));
            }
        }
        return Optional.empty();
    }

    private String resolveSourceKind(String fileId) {
        return arcFileContentMapper.selectById(fileId) != null ? "archive" : "original";
    }

    private boolean isOfd(String fileType, String fileName) {
        String normalizedType = normalize(fileType);
        String normalizedName = normalize(fileName);
        return "ofd".equals(normalizedType) || normalizedName.endsWith(".ofd");
    }

    private boolean isConvertedArtifact(String fileType, String fileName) {
        String ext = getExtension(fileName);
        String normalizedType = normalize(fileType);
        return CONVERTED_EXTENSIONS.contains(ext) || CONVERTED_EXTENSIONS.contains(normalizedType);
    }

    private boolean hasSameBaseName(String expectedBaseName, String fileName) {
        return expectedBaseName.equalsIgnoreCase(getBaseName(fileName));
    }

    private int compareConvertedPriority(String leftName, String rightName) {
        return Integer.compare(priority(rightName), priority(leftName));
    }

    private int priority(String fileName) {
        String ext = getExtension(fileName);
        if ("pdf".equals(ext)) {
            return 100;
        }
        if ("png".equals(ext)) {
            return 90;
        }
        if ("jpg".equals(ext) || "jpeg".equals(ext)) {
            return 80;
        }
        return 0;
    }

    private String buildDownloadUrl(String sourceKind, String fileId) {
        return "archive".equals(sourceKind)
            ? "/api/archive/files/download/" + fileId
            : "/api/original-vouchers/files/download/" + fileId;
    }

    private String toMimeType(String fileType, String fileName) {
        String normalizedType = normalize(fileType);
        String ext = StringUtils.hasText(normalizedType) ? normalizedType : getExtension(fileName);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            default -> "application/octet-stream";
        };
    }

    private String getBaseName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "";
        }
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex > 0 ? fileName.substring(0, dotIndex) : fileName;
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private record ResolvedConvertedArtifact(
        String fileId,
        String mimeType,
        String previewUrl
    ) {
    }
}
