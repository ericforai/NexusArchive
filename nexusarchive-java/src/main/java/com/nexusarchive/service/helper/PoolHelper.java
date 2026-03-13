// Input: Spring JDBC, PoolService, VoucherPdfGeneratorService
// Output: PoolHelper (凭证池辅助类)
// Pos: Service Helper Layer
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.helper;

import com.nexusarchive.dto.MetadataUpdateDTO;
import com.nexusarchive.dto.PoolItemDto;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.ArcFileMetadataIndex;
import com.nexusarchive.controller.PoolController.PoolItemDetailDto;
import com.nexusarchive.service.PoolService;
import com.nexusarchive.service.VoucherPdfGeneratorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class PoolHelper {

    private final PoolService poolService;
    private final JdbcTemplate jdbcTemplate;
    private final VoucherPdfGeneratorService pdfGeneratorService;
    private static final String[] SYSTEMS = {"Web上传", "用友", "金蝶", "泛微OA", "易快报", "汇联易", "SAP"};

    public PoolItemDetailDto mapToDetail(ArcFileContent f) {
        PoolItemDetailDto d = new PoolItemDetailDto();
        d.setId(f.getId()); d.setFileName(f.getFileName()); d.setFileType(f.getFileType());
        d.setFileSize(f.getFileSize()); d.setStatus(f.getPreArchiveStatus());
        d.setCreatedTime(f.getCreatedTime()); d.setFiscalYear(f.getFiscalYear());
        d.setVoucherType(f.getVoucherType()); d.setCreator(f.getCreator());
        d.setFondsCode(f.getFondsCode()); d.setSourceSystem(f.getSourceSystem());
        return d;
    }

    public void updateFields(MetadataUpdateDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("DTO cannot be null");
        }
        if (dto.getId() == null || dto.getId().isBlank()) {
            throw new IllegalArgumentException("File ID is required");
        }
        int updated = jdbcTemplate.update("UPDATE arc_file_content SET fiscal_year = ?, voucher_type = ?, creator = ?, fonds_code = ? WHERE id = ?",
                dto.getFiscalYear(), dto.getVoucherType(), dto.getCreator(), dto.getFondsCode(), dto.getId());
        if (updated == 0) {
            throw new IllegalArgumentException("File not found with ID: " + dto.getId());
        }
    }

    public void generateDemo() throws Exception {
        ClassPathResource res = new ClassPathResource("templates/default_voucher.pdf");
        if (!res.exists()) throw new RuntimeException("Template missing");
        poolService.cleanupDemoData();
        Random rnd = new Random();
        String date = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        for (int i = 0; i < 10; i++) {
            String fid = UUID.randomUUID().toString();
            Path path = Paths.get("/tmp/nexusarchive/uploads", fid + ".pdf");
            Files.createDirectories(path.getParent());
            try (var is = res.getInputStream()) { Files.copy(is, path, StandardCopyOption.REPLACE_EXISTING); }
            ArcFileContent c = ArcFileContent.builder().id(fid).archivalCode("TEMP-POOL-" + date + "-" + fid.substring(0, 8).toUpperCase())
                    .fileName("凭证_" + date + "_" + (1000 + i) + ".pdf").fileType("PDF").fileSize(Files.size(path))
                    .fileHash("DEMO_HASH_" + fid.substring(0, 8) + "_" + rnd.nextInt(SYSTEMS.length)).hashAlgorithm("SHA-256")
                    .storagePath(path.toString()).createdTime(LocalDateTime.now().minusMinutes(rnd.nextInt(60))).build();
            poolService.insertDemoFile(c);
            poolService.insertDemoMetadata(ArcFileMetadataIndex.builder().fileId(fid).totalAmount(new BigDecimal("43758.00"))
                    .invoiceNumber("INV-" + date + "-" + (1000 + i)).issueDate(java.time.LocalDate.now())
                    .sellerName("Demo Vendor " + (char)('A' + rnd.nextInt(26))).parsedTime(LocalDateTime.now()).parserType("DEMO").build());
        }
    }

    public Resource loadPreview(String id, String path, String name, String data) throws Exception {
        Path p = Paths.get(path);
        Resource r = new UrlResource(p.toUri());
        if (!r.exists() && name.toLowerCase().endsWith(".pdf")) {
            pdfGeneratorService.generatePdfForPreArchive(id, (data != null && !data.isEmpty()) ? data : "{}");
            r = new UrlResource(p.toUri());
        }
        return r;
    }
}
