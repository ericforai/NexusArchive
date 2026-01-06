// Input: Spring Framework, Java NIO
// Output: BatchFileStorageService
// Pos: Service Layer

package com.nexusarchive.service.collection;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * 批量文件存储服务
 *
 * 负责文件存储路径管理、文件I/O操作
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BatchFileStorageService {

    @Value("${collection.upload.root:/tmp/nexusarchive/uploads}")
    private String uploadRootPath;

    /**
     * 保存文件到存储
     *
     * @param inputStream 文件输入流
     * @param fondsCode 全宗代码
     * @param fiscalYear 会计年度
     * @param batchNo 批次编号
     * @param fileExtension 文件扩展名
     * @return 文件ID
     * @throws IOException 如果保存失败
     */
    public String saveFile(InputStream inputStream, String fondsCode,
                          String fiscalYear, String batchNo,
                          String fileExtension) throws IOException {
        String fileId = UUID.randomUUID().toString();

        // 构建存储路径
        Path uploadPath = buildStoragePath(fondsCode, fiscalYear, batchNo);
        Files.createDirectories(uploadPath);

        // 保存文件
        String targetFileName = fileId + "." + fileExtension;
        Path targetPath = uploadPath.resolve(targetFileName);
        Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);

        log.info("文件保存成功: fileId={}, path={}", fileId, targetPath);
        return fileId;
    }

    /**
     * 保存文件到存储（MultipartFile版本）
     */
    public String saveFile(MultipartFile file, String fondsCode,
                          String fiscalYear, String batchNo,
                          String fileExtension) throws IOException {
        return saveFile(file.getInputStream(), fondsCode, fiscalYear, batchNo, fileExtension);
    }

    /**
     * 构建存储路径
     */
    public Path buildStoragePath(String fondsCode, String fiscalYear, String batchNo) {
        return Paths.get(uploadRootPath, fondsCode, fiscalYear, batchNo);
    }

    /**
     * 生成完整存储路径字符串
     */
    public String buildStoragePathString(String fondsCode, String fiscalYear,
                                         String batchNo, String fileId,
                                         String fileExtension) {
        return Paths.get(uploadRootPath, fondsCode, fiscalYear, batchNo,
                        fileId + "." + fileExtension).toString();
    }

    /**
     * 生成唯一文件ID
     */
    public String generateFileId() {
        return UUID.randomUUID().toString();
    }
}
