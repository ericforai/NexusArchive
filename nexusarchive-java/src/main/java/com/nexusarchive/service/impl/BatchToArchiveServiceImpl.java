// Input: Spring Framework、Lombok、Java 标准库、本地模块
// Output: BatchToArchiveServiceImpl 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.CollectionBatch;
import com.nexusarchive.entity.CollectionBatchFile;
import com.nexusarchive.entity.enums.PreArchiveStatus;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.mapper.CollectionBatchFileMapper;
import com.nexusarchive.service.ArchivalCodeGenerator;
import com.nexusarchive.service.BatchToArchiveService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 批次文件转换服务实现
 *
 * <p>负责将批量上传的原始凭证附件转换为档案记录。</p>
 *
 * <p>符合 DA/T 94-2022《电子会计档案管理规范》第14条要求：
 * 在电子会计资料归档和电子会计档案管理过程中应同时捕获、归档和管理元数据。</p>
 *
 * <p>实现策略：</p>
 * <ul>
 *   <li>上传完成后立即创建档案记录（PENDING_METADATA状态）</li>
 *   <li>固定分类为 AC04（其他会计资料/原始凭证附件）</li>
 *   <li>异步触发智能解析更新元数据</li>
 *   <li>前端引导用户到凭证关联页面</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BatchToArchiveServiceImpl implements BatchToArchiveService {

    private final ArchiveMapper archiveMapper;
    private final CollectionBatchFileMapper batchFileMapper;
    private final ArchivalCodeGenerator archivalCodeGenerator;

    /**
     * 固定分类代码：AC04 - 其他会计资料（原始凭证附件）
     */
    private static final String CATEGORY_ATTACHMENT = "AC04";

    /**
     * 默认保管期限：30年
     */
    private static final String DEFAULT_RETENTION = "30Y";

    /**
     * 从批次信息创建档案记录
     *
     * @param fileContent 文件内容
     * @param batch 批次信息
     * @return 创建的档案记录
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Archive createArchiveFromBatch(ArcFileContent fileContent, CollectionBatch batch) {
        log.info("创建档案记录: fileId={}, batchNo={}", fileContent.getId(), batch.getBatchNo());

        // 生成档号
        String archiveCode = archivalCodeGenerator.generate(
            batch.getFondsCode(),
            batch.getFiscalYear(),
            DEFAULT_RETENTION,
            CATEGORY_ATTACHMENT
        );

        // 创建档案记录（最小必填字段）
        Archive archive = new Archive();
        archive.setId(generateArchiveId());
        archive.setArchiveCode(archiveCode);
        archive.setFondsNo(batch.getFondsCode());
        archive.setCategoryCode(CATEGORY_ATTACHMENT); // 固定为"其他会计资料"（原始凭证附件）
        archive.setFiscalYear(batch.getFiscalYear());
        archive.setFiscalPeriod(batch.getFiscalPeriod());
        archive.setTitle(fileContent.getFileName()); // 初始使用文件名，用户可修改
        archive.setRetentionPeriod("30年"); // 默认保管期限
        archive.setOrgName("上传单位"); // TODO: 从全宗信息获取
        archive.setStatus(PreArchiveStatus.PENDING_METADATA.getCode());

        // 存储文件内容关联
        archive.setFixityValue(fileContent.getFileHash());
        archive.setFixityAlgo(fileContent.getHashAlgorithm() != null
            ? fileContent.getHashAlgorithm()
            : "SHA-256");

        archiveMapper.insert(archive);

        log.info("档案记录创建成功: archiveId={}, archiveCode={}", archive.getId(), archiveCode);

        return archive;
    }

    /**
     * 批量完成时，标记所有文件的预归档状态
     *
     * @param batchId 批次ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markBatchAsPendingMetadata(Long batchId) {
        log.info("标记批次文件状态为待补录: batchId={}", batchId);

        // 注意：档案记录已在文件上传时创建，这里只需确认状态
        // 实际的状态更新在智能解析完成后进行
        log.debug("批次文件状态标记完成: batchId={}", batchId);
    }

    /**
     * 根据文件ID获取关联的档案ID
     *
     * @param fileId 文件ID
     * @return 档案ID，不存在返回null
     */
    @Override
    public String getArchiveIdByFileId(String fileId) {
        CollectionBatchFile batchFile = batchFileMapper.selectByFileId(fileId);
        return batchFile != null ? batchFile.getArchiveId() : null;
    }

    /**
     * 生成档案ID
     *
     * @return 32位UUID（不含连字符）
     */
    private String generateArchiveId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
