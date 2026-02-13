// Input: MyBatis-Plus、Jackson、Lombok、Spring Framework、等
// Output: AutoAssociationService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.parser.ParsedInvoice;
import com.nexusarchive.dto.sip.VoucherHeadDto;
import com.nexusarchive.entity.ArcFileContent;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.entity.ArchiveRelation;
import com.nexusarchive.mapper.ArcFileContentMapper;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.service.strategy.MatchingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

/**
 * 自动关联服务 (The "Matchmaker")
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoAssociationService implements IAutoAssociationService {

    private final ArchiveMapper archiveMapper;
    private final ArcFileContentMapper arcFileContentMapper;
    private final IArchiveRelationService archiveRelationService;
    private final List<MatchingStrategy> matchingStrategies;
    private final ObjectMapper objectMapper;

    /**
     * 夜间定时任务
     */
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void runNightlyJob() {
        log.info("Starting nightly auto-association job...");
        // 获取所有未归档/草稿状态的凭证
        List<Archive> vouchers = archiveMapper.selectList(new LambdaQueryWrapper<Archive>()
                .eq(Archive::getCategoryCode, "AC01")
                .ne(Archive::getStatus, "ASSOCIATED")); // 假设 ASSOCIATED 是一个终态或中间态

        for (Archive voucher : vouchers) {
            triggerAssociation(voucher.getId());
        }
        log.info("Nightly auto-association job completed.");
    }

    /**
     * 触发单个凭证的自动关联
     */
    @Transactional
    public void triggerAssociation(String voucherId) {
        Archive voucher = archiveMapper.selectById(voucherId);
        if (voucher == null || !"AC01".equals(voucher.getCategoryCode())) {
            return;
        }

        // 获取同全宗、同年度的候选文件 (AC04 - 其他/发票)
        List<Archive> candidates = archiveMapper.selectList(new LambdaQueryWrapper<Archive>()
                .eq(Archive::getCategoryCode, "AC04")
                .eq(Archive::getFondsNo, voucher.getFondsNo())
                .eq(Archive::getFiscalYear, voucher.getFiscalYear()));

        boolean newMatchFound = false;

        for (Archive candidate : candidates) {
            // 检查是否已经关联
            long count = archiveRelationService.count(new LambdaQueryWrapper<ArchiveRelation>()
                    .eq(ArchiveRelation::getSourceId, voucherId)
                    .eq(ArchiveRelation::getTargetId, candidate.getId()));
            if (count > 0) {
                continue;
            }

            for (MatchingStrategy strategy : matchingStrategies) {
                int confidence = strategy.match(voucher, candidate);
                if (confidence > 0) {
                    log.info("Match found: Voucher[{}] - File[{}] via Strategy[{}] with Confidence[{}]",
                            voucher.getArchiveCode(), candidate.getArchiveCode(), strategy.getName(), confidence);

                    createRelation(voucherId, candidate.getId(), confidence);
                    newMatchFound = true;
                    break; // 只要有一个策略匹配成功，就建立关联 (或者可以取最高分)
                }
            }
        }

        if (newMatchFound) {
            checkAndUpdateVoucherStatus(voucher);
        }
    }

    private void createRelation(String sourceId, String targetId, int confidence) {
        ArchiveRelation relation = new ArchiveRelation();
        relation.setSourceId(sourceId);
        relation.setTargetId(targetId);
        relation.setRelationType("SYSTEM_AUTO");
        relation.setRelationDesc("Auto-match confidence: " + confidence);
        // relation.setCreatedBy("SYSTEM"); // 假设有系统用户或留空
        archiveRelationService.save(relation);
    }

    private void checkAndUpdateVoucherStatus(Archive voucher) {
        try {
            // 计算已关联发票的总金额
            List<ArchiveRelation> relations = archiveRelationService.list(new LambdaQueryWrapper<ArchiveRelation>()
                    .eq(ArchiveRelation::getSourceId, voucher.getId()));
            
            BigDecimal totalLinkedAmount = BigDecimal.ZERO;
            
            for (ArchiveRelation relation : relations) {
                Archive target = archiveMapper.selectById(relation.getTargetId());
                if (target != null && target.getStandardMetadata() != null) {
                    ParsedInvoice invoice = objectMapper.readValue(target.getStandardMetadata(), ParsedInvoice.class);
                    if (invoice.getTotalAmount() != null) {
                        totalLinkedAmount = totalLinkedAmount.add(invoice.getTotalAmount());
                    }
                }
            }

            // 获取凭证总金额
            if (voucher.getStandardMetadata() != null) {
                VoucherHeadDto voucherHead = objectMapper.readValue(voucher.getStandardMetadata(), VoucherHeadDto.class);
                if (voucherHead.getTotalAmount() != null) {
                    // 如果已关联金额 >= 凭证金额，标记为 ASSOCIATED
                    if (totalLinkedAmount.compareTo(voucherHead.getTotalAmount()) >= 0) {
                        voucher.setStatus("ASSOCIATED");
                        archiveMapper.updateById(voucher);
                        log.info("Voucher[{}] status updated to ASSOCIATED", voucher.getArchiveCode());
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error updating voucher status: {}", e.getMessage());
        }
    }
    @Override
    public List<com.nexusarchive.dto.relation.LinkedFileDto> getLinkedFiles(String voucherId) {
        List<ArchiveRelation> relations = archiveRelationService.list(new LambdaQueryWrapper<ArchiveRelation>()
                .eq(ArchiveRelation::getSourceId, voucherId));
        
        List<com.nexusarchive.dto.relation.LinkedFileDto> result = new java.util.ArrayList<>();
        
        for (ArchiveRelation relation : relations) {
            Archive target = archiveMapper.selectById(relation.getTargetId());
            if (target != null) {
                // 只返回真实可下载文件，避免返回历史占位链接导致前端 404
                List<ArcFileContent> files = arcFileContentMapper.selectList(
                        new LambdaQueryWrapper<ArcFileContent>()
                                .eq(ArcFileContent::getItemId, target.getId())
                );

                for (ArcFileContent file : files) {
                    if (file == null || file.getId() == null) {
                        continue;
                    }
                    result.add(com.nexusarchive.dto.relation.LinkedFileDto.builder()
                            .id(file.getId())
                            .name(file.getFileName() != null ? file.getFileName() : target.getTitle())
                            .type("invoice")
                            .url("/archive/files/download/" + file.getId())
                            .uploadDate(file.getCreatedTime() != null ? file.getCreatedTime().toLocalDate().toString() : "")
                            .size(file.getFileSize() != null ? String.valueOf(file.getFileSize()) : "Unknown")
                            .build());
                }
            }
        }
        return result;
    }
}
