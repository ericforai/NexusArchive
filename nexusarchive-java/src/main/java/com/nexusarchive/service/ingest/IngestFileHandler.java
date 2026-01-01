// Input: Spring Framework, Lombok
// Output: IngestFileHandler 类
// Pos: 服务层 - SIP 文件处理器

package com.nexusarchive.service.ingest;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.sip.AccountingSipDto;
import com.nexusarchive.dto.sip.AttachmentDto;
import com.nexusarchive.util.PathSecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * SIP 文件处理器
 * <p>
 * 负责处理 SIP 请求中的附件文件：
 * - 文件类型验证
 * - Base64 解码
 * - 临时文件落地
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IngestFileHandler {

    private final PathSecurityUtils pathSecurityUtils;

    @Value("${archive.temp.path:/tmp/nexusarchive}")
    private String tempRootPath;

    /**
     * 准备临时文件
     *
     * @param sipDto SIP 请求
     * @param requestId 请求 ID
     * @return 文件名 -> 文件内容的映射
     * @throws IOException 文件操作失败时抛出
     */
    public Map<String, byte[]> prepareTempFiles(AccountingSipDto sipDto, String requestId) throws IOException {
        Map<String, byte[]> fileStreams = new HashMap<>();
        String tempPath = Paths.get(tempRootPath, requestId).toString();

        if (sipDto.getAttachments() == null) {
            return fileStreams;
        }

        Files.createDirectories(Paths.get(tempPath));

        // 文件类型验证器
        com.nexusarchive.util.FileMagicValidator fileMagicValidator =
                new com.nexusarchive.util.FileMagicValidator();

        for (AttachmentDto attachment : sipDto.getAttachments()) {
            try {
                String safeFileName = pathSecurityUtils.getSafeFileName(attachment.getFileName());
                if (fileStreams.containsKey(safeFileName)) {
                    throw new BusinessException(400, "重复附件文件名: " + safeFileName);
                }

                byte[] decoded = cn.hutool.core.codec.Base64.decode(attachment.getBase64Content());

                // 验证文件类型与扩展名是否匹配
                com.nexusarchive.util.FileMagicValidator.ValidationResult validationResult =
                        fileMagicValidator.validate(decoded, safeFileName);
                if (!validationResult.isValid()) {
                    log.warn("文件类型验证失败: {} - {}", safeFileName, validationResult.getMessage());
                    throw new BusinessException(400, "文件类型验证失败: " + validationResult.getMessage());
                }

                fileStreams.put(safeFileName, decoded);
                attachment.setFileName(safeFileName);

                // 写入临时文件
                Files.write(Paths.get(tempPath, safeFileName), decoded);

            } catch (BusinessException e) {
                throw e;
            } catch (Exception e) {
                throw new BusinessException(
                        Integer.parseInt(com.nexusarchive.common.constant.ErrorCode.EAA_1006_BASE64_ERROR.replace("EAA_", "")),
                        String.format(com.nexusarchive.common.constant.ErrorCode.EAA_1006_MSG, attachment.getFileName()));
            }
        }

        log.info("成功准备 {} 个临时文件", fileStreams.size());
        return fileStreams;
    }

    /**
     * 获取临时路径
     */
    public String getTempPath(String requestId) {
        return Paths.get(tempRootPath, requestId).toString();
    }
}
