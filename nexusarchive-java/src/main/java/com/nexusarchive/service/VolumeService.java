// Input: MyBatis-Plus、Lombok、Apache、Spring Framework、等
// Output: VolumeService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.Volume;
import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.VolumeMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * 组卷服务
 * 符合 DA/T 104-2024 第7.4节组卷规范
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class VolumeService {

    private final VolumeMapper volumeMapper;
    private final ArchiveMapper archiveMapper;
    
    @Value("${archive.root.path:/data/archives}")
    private String archiveRootPath;

    /**
     * 按月自动组卷
     * 规范: "业务单据一般按月进行组卷"
     *
     * @param fiscalPeriod 会计期间 (YYYY-MM)
     * @return 创建的案卷
     */
    @Transactional
    public Volume assembleByMonth(String fiscalPeriod) {
        log.info("开始按月组卷: {}", fiscalPeriod);
        
        // 1. 查询该期间内未组卷的凭证
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getFiscalPeriod, fiscalPeriod)
               .eq(Archive::getCategoryCode, "AC01") // 会计凭证
               .isNull(Archive::getVolumeId)
               .eq(Archive::getDeleted, 0);
        
        List<Archive> archives = archiveMapper.selectList(wrapper);
        
        if (archives.isEmpty()) {
            log.info("该期间无待组卷凭证: {}", fiscalPeriod);
            throw new BusinessException("该期间没有待组卷的凭证");
        }
        
        // 2. 获取全宗号和组织名称
        String fondsNo = archives.get(0).getFondsNo();
        String orgName = archives.get(0).getOrgName();
        String fiscalYear = fiscalPeriod.substring(0, 4);
        String month = fiscalPeriod.substring(5, 7);
        
        // 3. 生成案卷号 (格式: 全宗号-分类号-年月序号)
        String volumeCode = generateVolumeCode(fondsNo, "AC01", fiscalPeriod);
        
        // 4. 生成案卷标题 (格式: 责任者+年度+月度+业务单据名称)
        String title = String.format("%s%s年%s月会计凭证", 
                orgName != null ? orgName : fondsNo, fiscalYear, month);
        
        // 5. 计算保管期限 (取最长)
        String retentionPeriod = calculateMaxRetention(archives);
        
        // 6. 创建案卷
        Volume volume = new Volume();
        volume.setId(UUID.randomUUID().toString().replace("-", ""));
        volume.setVolumeCode(volumeCode);
        volume.setTitle(title);
        volume.setFondsNo(fondsNo);
        volume.setFiscalYear(fiscalYear);
        volume.setFiscalPeriod(fiscalPeriod);
        volume.setCategoryCode("AC01");
        volume.setFileCount(archives.size());
        volume.setRetentionPeriod(retentionPeriod);
        volume.setStatus("draft");
        volume.setCreatedTime(LocalDateTime.now());
        volume.setLastModifiedTime(LocalDateTime.now());
        
        volumeMapper.insert(volume);
        log.info("案卷创建成功: {} - {}", volumeCode, title);
        
        // 7. 更新凭证的案卷关联
        for (Archive archive : archives) {
            archive.setVolumeId(volume.getId());
            archive.setLastModifiedTime(LocalDateTime.now());
            archiveMapper.updateById(archive);
        }
        
        log.info("组卷完成: 案卷={}, 凭证数={}", volumeCode, archives.size());
        return volume;
    }

    /**
     * 生成案卷号
     * 格式: 全宗号-分类号-期间序号 (如 BR01-AC01-202508)
     */
    private String generateVolumeCode(String fondsNo, String categoryCode, String fiscalPeriod) {
        String periodCode = fiscalPeriod.replace("-", "");
        return String.format("%s-%s-%s", fondsNo, categoryCode, periodCode);
    }

    /**
     * 计算最长保管期限
     * 规范: "保管期限按卷内文件的最长保管期限填写"
     */
    private String calculateMaxRetention(List<Archive> archives) {
        Map<String, Integer> priorityMap = Map.of(
                "PERMANENT", 3,
                "30Y", 2,
                "10Y", 1
        );
        
        return archives.stream()
                .map(Archive::getRetentionPeriod)
                .filter(Objects::nonNull)
                .max(Comparator.comparingInt(r -> priorityMap.getOrDefault(r, 0)))
                .orElse("10Y");
    }

    /**
     * 获取案卷列表
     */
    public Page<Volume> getVolumeList(int page, int limit, String status) {
        Page<Volume> pageObj = new Page<>(page, limit);
        LambdaQueryWrapper<Volume> wrapper = new LambdaQueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            wrapper.eq(Volume::getStatus, status);
        }
        
        wrapper.orderByDesc(Volume::getCreatedTime);
        return volumeMapper.selectPage(pageObj, wrapper);
    }

    /**
     * 获取案卷详情
     */
    public Volume getVolumeById(String volumeId) {
        return volumeMapper.selectById(volumeId);
    }

    /**
     * 获取卷内文件列表
     * 规范: "卷内文件一般按照形成时间顺序排列"
     */
    public List<Archive> getVolumeFiles(String volumeId) {
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getVolumeId, volumeId)
               .eq(Archive::getDeleted, 0)
               .orderByAsc(Archive::getDocDate)   // 按凭证日期
               .orderByAsc(Archive::getArchiveCode); // 按档号
        return archiveMapper.selectList(wrapper);
    }

    /**
     * 提交案卷审核
     */
    @Transactional
    public void submitForReview(String volumeId) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        if (!"draft".equals(volume.getStatus())) {
            throw new BusinessException("只有草稿状态的案卷可以提交审核");
        }
        
        volume.setStatus("pending");
        volume.setLastModifiedTime(LocalDateTime.now());
        volumeMapper.updateById(volume);
        log.info("案卷已提交审核: {}", volume.getVolumeCode());
    }

    /**
     * 审核通过并归档
     * 规范: "对整理阶段划定的保管期限、分类结果及排序等内容进行审核和确认"
     */
    @Transactional
    public void approveArchival(String volumeId, String reviewerId) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        if (!"pending".equals(volume.getStatus())) {
            throw new BusinessException("只有待审核状态的案卷可以审批");
        }
        
        LocalDateTime now = LocalDateTime.now();
        volume.setStatus("archived");
        volume.setReviewedBy(reviewerId);
        volume.setReviewedAt(now);
        volume.setArchivedAt(now);
        volume.setLastModifiedTime(now);
        volumeMapper.updateById(volume);
        
        // 更新卷内凭证状态为已归档
        LambdaQueryWrapper<Archive> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Archive::getVolumeId, volumeId);
        List<Archive> archives = archiveMapper.selectList(wrapper);
        for (Archive archive : archives) {
            archive.setStatus("archived");
            archive.setLastModifiedTime(now);
            archiveMapper.updateById(archive);
        }
        
        log.info("案卷归档完成: {}", volume.getVolumeCode());
    }

    /**
     * 审核驳回
     */
    @Transactional
    public void rejectArchival(String volumeId, String reviewerId, String reason) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        if (!"pending".equals(volume.getStatus())) {
            throw new BusinessException("只有待审核状态的案卷可以驳回");
        }
        
        volume.setStatus("draft");
        volume.setReviewedBy(reviewerId);
        volume.setReviewedAt(LocalDateTime.now());
        volume.setLastModifiedTime(LocalDateTime.now());
        volumeMapper.updateById(volume);
        
        log.info("案卷审核驳回: {}, 原因: {}", volume.getVolumeCode(), reason);
    }

    /**
     * 生成归档登记表数据
     * 参照 GB/T 18894-2016 附录 A 的表 A.1
     */
    public Map<String, Object> generateRegistrationForm(String volumeId) {
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        
        List<Archive> files = getVolumeFiles(volumeId);
        
        Map<String, Object> form = new LinkedHashMap<>();
        form.put("registrationNo", "GD-" + volume.getFiscalPeriod() + "-" + System.currentTimeMillis() % 10000);
        form.put("volumeCode", volume.getVolumeCode());
        form.put("volumeTitle", volume.getTitle());
        form.put("fondsNo", volume.getFondsNo());
        form.put("fiscalYear", volume.getFiscalYear());
        form.put("fiscalPeriod", volume.getFiscalPeriod());
        form.put("categoryCode", volume.getCategoryCode());
        form.put("categoryName", "会计凭证");
        form.put("fileCount", volume.getFileCount());
        form.put("retentionPeriod", volume.getRetentionPeriod());
        form.put("registrationTime", LocalDateTime.now().toString());
        form.put("status", volume.getStatus());
        
        // 卷内文件清单
        List<Map<String, Object>> fileList = files.stream().map(f -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("序号", files.indexOf(f) + 1);
            item.put("档号", f.getArchiveCode());
            item.put("题名", f.getTitle());
            item.put("日期", f.getDocDate() != null ? f.getDocDate().toString() : "");
            item.put("金额", f.getAmount());
            item.put("制单人", f.getCreator());
            item.put("保管期限", f.getRetentionPeriod());
            return item;
        }).collect(Collectors.toList());
        
        form.put("fileList", fileList);
        
        return form;
    }
    
    /**
     * 移交档案管理部门
     * 规范: "会计年度终了后...移交单位档案管理机构保管"
     */
    @Transactional
    public void handoverToArchives(String volumeId) {
        log.info("开始移交案卷至档案部门: {}", volumeId);
        
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        
        if (!"archived".equals(volume.getStatus())) {
            throw new BusinessException("只有已归档的案卷可以移交");
        }
        
        if ("ARCHIVES".equals(volume.getCustodianDept())) {
            throw new BusinessException("案卷已在档案部门保管中");
        }
        
        volume.setCustodianDept("ARCHIVES");
        volume.setLastModifiedTime(LocalDateTime.now());
        volumeMapper.updateById(volume);
        
        log.info("案卷移交完成: {}", volume.getVolumeCode());
    }

    /**
     * 导出案卷 AIP 包
     * 符合 DA/T 94-2022 和 GB/T 39674 标准
     * 
     * AIP 包结构:
     * /AIP_Root
     *   ├── index.xml          - 总索引文件
     *   ├── /metadata          - 元数据目录
     *   │    └── volume.xml    - 案卷元数据
     *   ├── /content           - 内容文件目录 (凭证 PDF/OFD)
     *   └── /logs              - 审计日志
     *        └── audit.xml     - 归档审计日志
     */
    public File exportAipPackage(String volumeId) throws IOException {
        log.info("开始导出案卷 AIP 包: volumeId={}", volumeId);
        
        Volume volume = volumeMapper.selectById(volumeId);
        if (volume == null) {
            throw new BusinessException("案卷不存在");
        }
        
        if (!"archived".equals(volume.getStatus())) {
            throw new BusinessException("只有已归档的案卷可以导出 AIP 包");
        }
        
        List<Archive> files = getVolumeFiles(volumeId);
        
        // 创建临时目录
        Path tempDir = Files.createTempDirectory("aip_" + volume.getVolumeCode() + "_");
        
        try {
            // 1. 创建目录结构
            Path metadataDir = Files.createDirectories(tempDir.resolve("metadata"));
            Path contentDir = Files.createDirectories(tempDir.resolve("content"));
            Path logsDir = Files.createDirectories(tempDir.resolve("logs"));
            
            // 2. 生成 index.xml (总索引)
            String indexXml = generateIndexXml(volume, files);
            Files.writeString(tempDir.resolve("index.xml"), indexXml);
            
            // 3. 生成 volume.xml (案卷元数据)
            String volumeXml = generateVolumeXml(volume, files);
            Files.writeString(metadataDir.resolve("volume.xml"), volumeXml);
            
            // 4. 收集内容文件
            int fileIndex = 1;
            for (Archive archive : files) {
                // 查找实际文件路径
                String filename = String.format("%03d_%s.pdf", fileIndex++, archive.getArchiveCode().replace("/", "_"));
                Path targetPath = contentDir.resolve(filename);
                
                // 尝试从存储路径复制文件
                String sourcePath = archiveRootPath + "/" + archive.getFondsNo() + "/" + archive.getArchiveCode() + ".pdf";
                Path source = Paths.get(sourcePath);
                if (Files.exists(source)) {
                    Files.copy(source, targetPath, StandardCopyOption.REPLACE_EXISTING);
                } else {
                    // 如果实际文件不存在，创建合规的 PDF 占位文件
                    generatePlaceholderPdf(targetPath, archive);
                }
            }
            
            // 5. 生成审计日志
            String auditXml = generateAuditXml(volume, files);
            Files.writeString(logsDir.resolve("audit.xml"), auditXml);
            
            // 6. 打包为 ZIP
            String zipFileName = volume.getVolumeCode() + "_AIP.zip";
            File zipFile = File.createTempFile(volume.getVolumeCode() + "_", ".zip");
            
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile), java.nio.charset.StandardCharsets.UTF_8)) {
                zipDirectory(tempDir, "", zos);
            }
            
            log.info("AIP 包导出完成: {}, 大小: {} bytes", volume.getVolumeCode(), zipFile.length());
            return zipFile;
            
        } finally {
            // 清理临时目录
            deleteDirectoryRecursively(tempDir);
        }
    }
    
    /**
     * 生成 index.xml (总索引)
     */
    private String generateIndexXml(Volume volume, List<Archive> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<aip_package xmlns=\"http://www.saac.gov.cn/dat94/2022\">\n");
        sb.append("  <header>\n");
        sb.append("    <version>1.0</version>\n");
        sb.append("    <standard>DA/T 94-2022</standard>\n");
        sb.append("    <created_time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</created_time>\n");
        sb.append("  </header>\n");
        sb.append("  <volume_info>\n");
        sb.append("    <volume_code>").append(volume.getVolumeCode()).append("</volume_code>\n");
        sb.append("    <title>").append(escapeXml(volume.getTitle())).append("</title>\n");
        sb.append("    <fonds_no>").append(volume.getFondsNo()).append("</fonds_no>\n");
        sb.append("    <fiscal_year>").append(volume.getFiscalYear()).append("</fiscal_year>\n");
        sb.append("    <fiscal_period>").append(volume.getFiscalPeriod()).append("</fiscal_period>\n");
        sb.append("    <category_code>").append(volume.getCategoryCode()).append("</category_code>\n");
        sb.append("    <retention_period>").append(volume.getRetentionPeriod()).append("</retention_period>\n");
        sb.append("    <file_count>").append(files.size()).append("</file_count>\n");
        sb.append("    <archived_at>").append(volume.getArchivedAt() != null ? volume.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "").append("</archived_at>\n");
        sb.append("    <reviewed_by>").append(volume.getReviewedBy() != null ? volume.getReviewedBy() : "").append("</reviewed_by>\n");
        sb.append("  </volume_info>\n");
        sb.append("  <file_list>\n");
        
        int seq = 1;
        for (Archive f : files) {
            sb.append("    <file seq=\"").append(seq++).append("\">\n");
            sb.append("      <archive_code>").append(f.getArchiveCode()).append("</archive_code>\n");
            sb.append("      <title>").append(escapeXml(f.getTitle())).append("</title>\n");
            sb.append("      <doc_date>").append(f.getDocDate() != null ? f.getDocDate().toString() : "").append("</doc_date>\n");
            sb.append("      <amount>").append(f.getAmount() != null ? f.getAmount().toString() : "0").append("</amount>\n");
            sb.append("      <creator>").append(escapeXml(f.getCreator())).append("</creator>\n");
            sb.append("      <retention_period>").append(f.getRetentionPeriod()).append("</retention_period>\n");
            sb.append("      <content_path>content/").append(String.format("%03d_%s.pdf", seq - 1, f.getArchiveCode().replace("/", "_"))).append("</content_path>\n");
            sb.append("    </file>\n");
        }
        
        sb.append("  </file_list>\n");
        sb.append("</aip_package>\n");
        return sb.toString();
    }
    
    /**
     * 生成 volume.xml (案卷元数据)
     */
    private String generateVolumeXml(Volume volume, List<Archive> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<volume xmlns=\"http://www.saac.gov.cn/dat94/2022\">\n");
        sb.append("  <id>").append(volume.getId()).append("</id>\n");
        sb.append("  <volume_code>").append(volume.getVolumeCode()).append("</volume_code>\n");
        sb.append("  <title>").append(escapeXml(volume.getTitle())).append("</title>\n");
        sb.append("  <fonds_no>").append(volume.getFondsNo()).append("</fonds_no>\n");
        sb.append("  <fiscal_year>").append(volume.getFiscalYear()).append("</fiscal_year>\n");
        sb.append("  <fiscal_period>").append(volume.getFiscalPeriod()).append("</fiscal_period>\n");
        sb.append("  <category_code>").append(volume.getCategoryCode()).append("</category_code>\n");
        sb.append("  <category_name>会计凭证</category_name>\n");
        sb.append("  <file_count>").append(volume.getFileCount()).append("</file_count>\n");
        sb.append("  <retention_period>").append(volume.getRetentionPeriod()).append("</retention_period>\n");
        sb.append("  <status>").append(volume.getStatus()).append("</status>\n");
        sb.append("  <created_time>").append(volume.getCreatedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</created_time>\n");
        sb.append("  <archived_at>").append(volume.getArchivedAt() != null ? volume.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) : "").append("</archived_at>\n");
        sb.append("  <reviewed_by>").append(volume.getReviewedBy() != null ? volume.getReviewedBy() : "").append("</reviewed_by>\n");
        sb.append("</volume>\n");
        return sb.toString();
    }
    
    /**
     * 生成审计日志 XML
     */
    private String generateAuditXml(Volume volume, List<Archive> files) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<audit_log xmlns=\"http://www.saac.gov.cn/dat94/2022\">\n");
        sb.append("  <export_time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</export_time>\n");
        sb.append("  <volume_code>").append(volume.getVolumeCode()).append("</volume_code>\n");
        sb.append("  <operation>AIP_EXPORT</operation>\n");
        sb.append("  <events>\n");
        sb.append("    <event>\n");
        sb.append("      <time>").append(volume.getCreatedTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</time>\n");
        sb.append("      <action>VOLUME_CREATED</action>\n");
        sb.append("      <description>案卷创建</description>\n");
        sb.append("    </event>\n");
        if (volume.getArchivedAt() != null) {
            sb.append("    <event>\n");
            sb.append("      <time>").append(volume.getArchivedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</time>\n");
            sb.append("      <action>VOLUME_ARCHIVED</action>\n");
            sb.append("      <operator>").append(volume.getReviewedBy() != null ? volume.getReviewedBy() : "").append("</operator>\n");
            sb.append("      <description>案卷归档</description>\n");
            sb.append("    </event>\n");
        }
        sb.append("    <event>\n");
        sb.append("      <time>").append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)).append("</time>\n");
        sb.append("      <action>AIP_EXPORTED</action>\n");
        sb.append("      <description>AIP包导出</description>\n");
        sb.append("      <file_count>").append(files.size()).append("</file_count>\n");
        sb.append("    </event>\n");
        sb.append("  </events>\n");
        sb.append("</audit_log>\n");
        return sb.toString();
    }
    
    /**
     * XML 特殊字符转义
     */
    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    /**
     * 递归打包目录到 ZIP
     */
    private void zipDirectory(Path sourceDir, String basePath, ZipOutputStream zos) throws IOException {
        try (var stream = Files.walk(sourceDir)) {
            stream.filter(path -> !Files.isDirectory(path))
                  .forEach(path -> {
                      String zipEntryName = basePath.isEmpty() 
                          ? sourceDir.relativize(path).toString()
                          : basePath + "/" + sourceDir.relativize(path).toString();
                      try {
                          zos.putNextEntry(new ZipEntry(zipEntryName));
                          Files.copy(path, zos);
                          zos.closeEntry();
                      } catch (IOException e) {
                          log.error("打包文件失败: {}", path, e);
                      }
                  });
        }
    }
    
    /**
     * 递归删除临时目录
     */
    private void deleteDirectoryRecursively(Path path) throws IOException {
        if (Files.exists(path)) {
            try (var stream = Files.walk(path)) {
                stream.sorted(Comparator.reverseOrder())
                      .forEach(p -> {
                          try {
                              Files.deleteIfExists(p);
                          } catch (IOException e) {
                              log.warn("删除临时文件失败: {}", p);
                          }
                      });
            }
        }
    }

    /**
     * 生成合规的 PDF 版式文件
     * 如果存在自定义元数据(包含分录详情)，则生成详细凭证；否则生成占位文件。
     */
    private void generatePlaceholderPdf(Path targetPath, Archive archive) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            // 尝试加载中文字体
            org.apache.pdfbox.pdmodel.font.PDFont chineseFont = null;
            org.apache.pdfbox.pdmodel.font.PDFont chineseFontBold = null;
            
            // 尝试从系统加载中文字体 (macOS, Linux, Windows 常见路径)
            String[] fontPaths = {
                // macOS
                "/System/Library/Fonts/STHeiti Light.ttc",
                "/System/Library/Fonts/STHeiti Medium.ttc",
                "/Library/Fonts/Arial Unicode.ttf",
                "/System/Library/Fonts/PingFang.ttc",
                // Linux
                "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/noto-cjk/NotoSansSC-Regular.otf",
                "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
                "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
                // Windows
                "C:/Windows/Fonts/msyh.ttc",
                "C:/Windows/Fonts/simsun.ttc",
                "C:/Windows/Fonts/simhei.ttf"
            };
            
            for (String fontPath : fontPaths) {
                try {
                    java.io.File fontFile = new java.io.File(fontPath);
                    if (fontFile.exists()) {
                        chineseFont = PDType0Font.load(document, fontFile);
                        chineseFontBold = chineseFont; // 使用相同字体作为粗体替代
                        log.debug("Loaded Chinese font from: {}", fontPath);
                        break;
                    }
                } catch (Exception e) {
                    log.trace("Failed to load font from: {}", fontPath);
                }
            }
            
            // 如果没有找到中文字体，回退到 ASCII 字体
            boolean useChinese = chineseFont != null;
            org.apache.pdfbox.pdmodel.font.PDFont regularFont = useChinese ? chineseFont : PDType1Font.HELVETICA;
            org.apache.pdfbox.pdmodel.font.PDFont boldFont = useChinese ? chineseFontBold : PDType1Font.HELVETICA_BOLD;

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                // 1. 标题
                contentStream.beginText();
                contentStream.setFont(boldFont, 18);
                contentStream.newLineAtOffset(160, 750);
                contentStream.showText(useChinese ? "会计凭证 - Accounting Voucher" : "ACCOUNTING VOUCHER");
                contentStream.endText();

                // 2. 头部信息
                contentStream.beginText();
                contentStream.setFont(regularFont, 10);
                contentStream.setLeading(14.5f);
                contentStream.newLineAtOffset(50, 720);
                
                String voucherNo = archive.getArchiveCode() != null ? archive.getArchiveCode() : "";
                String docDate = archive.getDocDate() != null ? archive.getDocDate().toString() : "";
                contentStream.showText((useChinese ? "凭证号: " : "Voucher No: ") + safeText(voucherNo, useChinese));
                contentStream.newLineAtOffset(300, 0);
                contentStream.showText((useChinese ? "日期: " : "Date: ") + safeText(docDate, useChinese));
                contentStream.endText();

                // 3. 分录表格
                float yPosition = 680;
                int margin = 50;
                
                // 表头
                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                contentStream.showText(useChinese ? "摘要" : "Description");
                contentStream.newLineAtOffset(200, 0);
                contentStream.showText(useChinese ? "科目" : "Subject");
                contentStream.newLineAtOffset(150, 0);
                contentStream.showText(useChinese ? "借方" : "Debit");
                contentStream.newLineAtOffset(100, 0);
                contentStream.showText(useChinese ? "贷方" : "Credit");
                contentStream.endText();
                
                yPosition -= 20;
                contentStream.moveTo(margin, yPosition + 15);
                contentStream.lineTo(550, yPosition + 15);
                contentStream.stroke();

                // 尝试解析详细分录
                boolean hasDetails = false;
                if (archive.getCustomMetadata() != null && !archive.getCustomMetadata().isEmpty()) {
                    try {
                        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                        com.fasterxml.jackson.databind.JsonNode bodies = mapper.readTree(archive.getCustomMetadata());
                        
                        if (bodies.isArray()) {
                            hasDetails = true;
                            contentStream.setFont(regularFont, 9);
                            
                            for (com.fasterxml.jackson.databind.JsonNode body : bodies) {
                                String desc = body.path("description").asText("");
                                String subject = body.path("accsubject").path("name").asText("");
                                // 处理可能不同的字段名 (list vs detail)
                                if (subject.isEmpty()) {
                                    subject = body.path("accSubject").asText("");
                                }
                                
                                double debit = body.path("debit_original").asDouble(0.0);
                                if (debit == 0.0) debit = body.path("debitOriginal").asDouble(0.0);
                                
                                double credit = body.path("credit_original").asDouble(0.0);
                                if (credit == 0.0) credit = body.path("creditOriginal").asDouble(0.0);

                                contentStream.beginText();
                                contentStream.newLineAtOffset(margin, yPosition);
                                contentStream.showText(safeText(truncateText(desc, 30), useChinese));
                                contentStream.newLineAtOffset(200, 0);
                                contentStream.showText(safeText(truncateText(subject, 20), useChinese));
                                contentStream.newLineAtOffset(150, 0);
                                if (debit != 0) contentStream.showText(String.format("%.2f", debit));
                                contentStream.newLineAtOffset(100, 0);
                                if (credit != 0) contentStream.showText(String.format("%.2f", credit));
                                contentStream.endText();
                                
                                yPosition -= 15;
                                if (yPosition < 50) { // 简单分页处理：只显示第一页
                                    break;
                                }
                            }
                        }
                    } catch (Exception e) {
                        // 解析失败，忽略详情
                        log.debug("Failed to parse customMetadata: {}", e.getMessage());
                    }
                }

                if (!hasDetails) {
                    contentStream.beginText();
                    contentStream.setFont(regularFont, 10);
                    contentStream.newLineAtOffset(margin, yPosition);
                    contentStream.showText(useChinese ? "暂无详细分录信息" : "No detailed entries available. (Metadata missing)");
                    contentStream.endText();
                }

                // 4. 底部合计
                yPosition -= 30;
                contentStream.moveTo(margin, yPosition + 25);
                contentStream.lineTo(550, yPosition + 25);
                contentStream.stroke();
                
                contentStream.beginText();
                contentStream.setFont(boldFont, 10);
                contentStream.newLineAtOffset(margin, yPosition);
                String amountStr = archive.getAmount() != null ? archive.getAmount().toString() : "0.00";
                String creatorStr = archive.getCreator() != null ? archive.getCreator() : "System";
                contentStream.showText((useChinese ? "合计金额: " : "Total Amount: ") + amountStr);
                contentStream.newLineAtOffset(300, 0);
                contentStream.showText((useChinese ? "制单人: " : "Creator: ") + safeText(creatorStr, useChinese));
                contentStream.endText();
                
                // 5. 脚注
                contentStream.beginText();
                contentStream.setFont(regularFont, 8);
                contentStream.newLineAtOffset(margin, 30);
                contentStream.showText(useChinese ? "由NexusArchive系统根据用友元数据自动生成" : "Generated by NexusArchive System based on YonSuite Metadata.");
                contentStream.endText();
            }

            document.save(targetPath.toFile());
        }
    }
    
    /**
     * 截断文本到指定长度
     */
    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
    
    /**
     * 安全文本处理
     * 如果支持中文字体，返回原文本
     * 如果不支持，替换非ASCII字符为?
     */
    private String safeText(String text, boolean supportChinese) {
        if (text == null) return "";
        if (supportChinese) return text;
        
        // 回退到 ASCII 模式
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c > 127) {
                sb.append("?");
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * PDF 文本清洗
     * 由于使用标准字体 Helvetica，仅支持 ASCII 字符。
     * 将非 ASCII 字符替换为 "?" 以防止 PDF 文件损坏。
     */
    private String sanitizeForPdf(String text) {
        if (text == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c > 127) {
                sb.append("?"); // 替换非 ASCII 字符
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
