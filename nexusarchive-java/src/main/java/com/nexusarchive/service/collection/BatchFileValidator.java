// Input: MyBatis-Plus, Java Standard Library
// Output: BatchFileValidator
// Pos: Service Layer

package com.nexusarchive.service.collection;

import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * 批量文件验证器
 *
 * 负责文件类型检测、重复检测、文件验证规则
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchFileValidator {

    private final CollectionBatchFileMapper batchFileMapper;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "pdf", "ofd", "xml", "jpg", "jpeg", "png", "tif", "tiff"
    );

    /**
     * 检测文件类型
     */
    public String detectFileType(String filename) {
        if (filename == null) return "UNKNOWN";
        String ext = getExtension(filename);
        return switch (ext.toLowerCase()) {
            case "pdf" -> "PDF";
            case "ofd" -> "OFD";
            case "xml" -> "XML";
            case "jpg", "jpeg" -> "JPG";
            case "png" -> "PNG";
            case "tif", "tiff" -> "TIFF";
            default -> "UNKNOWN";
        };
    }

    /**
     * 获取文件扩展名
     */
    public String getExtension(String filename) {
        if (filename == null || filename.isEmpty()) return "bin";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot + 1) : "bin";
    }

    /**
     * 检查文件类型是否允许
     */
    public boolean isAllowedFileType(String filename) {
        if (filename == null) return false;
        String ext = getExtension(filename).toLowerCase();
        return ALLOWED_EXTENSIONS.contains(ext);
    }

    /**
     * 检查重复文件
     *
     * @param fileHash 文件哈希值
     * @param fondsCode 全宗代码
     * @param fiscalYear 会计年度
     * @return 重复的文件记录，如果不存在则返回null
     */
    public CollectionBatchFile checkDuplicate(String fileHash, String fondsCode, String fiscalYear) {
        return batchFileMapper.findDuplicateByHash(fileHash, fondsCode, fiscalYear);
    }

    /**
     * 验证文件大小
     *
     * @param sizeBytes 文件大小（字节）
     * @param maxSizeBytes 最大允许大小（字节）
     * @return 是否在有效范围内
     */
    public boolean isValidFileSize(long sizeBytes, long maxSizeBytes) {
        return sizeBytes > 0 && sizeBytes <= maxSizeBytes;
    }

    /**
     * 获取允许的文件类型集合
     */
    public Set<String> getAllowedExtensions() {
        return Set.copyOf(ALLOWED_EXTENSIONS);
    }
}
