// Input: Spring Framework、Lombok、Java 标准库、等
// Output: PoolDemoDataService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.nexusarchive.common.result.Result;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

/**
 * 电子凭证池演示数据服务
 * 负责生成测试用的演示数据
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PoolDemoDataService {

    private final PoolService poolService;

    private static final String[] SOURCE_SYSTEMS = {
            "Web上传", "用友", "金蝶", "泛微OA", "易快报", "汇联易", "SAP"
    };

    /**
     * 生成演示数据
     *
     * @return 生成结果
     */
    public Result<String> generateDemoData() {
        log.info("开始生成演示数据...");

        try {
            ClassPathResource templateResource = new ClassPathResource("templates/default_voucher.pdf");
            if (!templateResource.exists()) {
                return Result.error("模板文件不存在(classpath): templates/default_voucher.pdf");
            }
            Random random = new Random();
            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
            String dateStr = LocalDateTime.now().format(dateFormatter);

            // 1. 清理旧的演示数据
            int deletedCount = poolService.cleanupDemoData();
            log.info("已清理 {} 条旧演示数据", deletedCount);

            // 2. 生成新数据
            for (int i = 0; i < 10; i++) {
                String fileId = UUID.randomUUID().toString();
                String targetFileName = fileId + ".pdf";
                Path targetPath = Paths.get("/tmp/nexusarchive/uploads", targetFileName);

                // 确保目录存在
                Files.createDirectories(targetPath.getParent());

                // 复制模板文件 (从Classpath读取)
                try (java.io.InputStream is = templateResource.getInputStream()) {
                    Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
                }

                // 使用固定金额 (与模板 default_voucher.pdf 一致)
                // 凭证金额 = 借方合计 = 贷方合计 (不能双算)
                BigDecimal amount = new BigDecimal("43758.00");

                // 随机来源系统 (0-6)
                int sourceIndex = random.nextInt(SOURCE_SYSTEMS.length);

                // 创建记录
                ArcFileContent content = ArcFileContent.builder()
                        .id(fileId)
                        .archivalCode("TEMP-POOL-" + dateStr + "-" + fileId.substring(0, 8).toUpperCase())
                        .fileName("凭证_" + dateStr + "_" + (1000 + i) + ".pdf")
                        .fileType("PDF")
                        .fileSize(Files.size(targetPath))
                        .fileHash("DEMO_HASH_" + fileId.substring(0, 8) + "_" + sourceIndex) // 演示数据用伪哈希 + 来源索引
                        .hashAlgorithm("SHA-256")
                        .storagePath(targetPath.toString())
                        .createdTime(LocalDateTime.now().minusMinutes(random.nextInt(60)))
                        .build();

                poolService.insertDemoFile(content);

                // 创建元数据索引 (包含金额)
                ArcFileMetadataIndex metadata = ArcFileMetadataIndex.builder()
                        .fileId(fileId)
                        .totalAmount(amount)
                        .invoiceNumber("INV-" + dateStr + "-" + (1000 + i))
                        .issueDate(java.time.LocalDate.now())
                        .sellerName("演示供应商 " + (char) ('A' + random.nextInt(26)))
                        .parsedTime(LocalDateTime.now())
                        .parserType("DEMO_GENERATOR")
                        .build();
                poolService.insertDemoMetadata(metadata);
            }

            return Result.success("成功生成10条演示数据");
        } catch (Exception e) {
            log.error("生成演示数据失败", e);
            return Result.error("生成失败: " + e.getMessage());
        }
    }
}
