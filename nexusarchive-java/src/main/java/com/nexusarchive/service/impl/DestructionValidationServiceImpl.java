// Input: DestructionValidationService, BorrowingMapper, ArchiveMapper
// Output: DestructionValidationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.entity.Archive;
import com.nexusarchive.exception.DestructionNotAllowedException;
import com.nexusarchive.mapper.ArchiveMapper;
import com.nexusarchive.modules.borrowing.domain.Borrowing;
import com.nexusarchive.modules.borrowing.domain.BorrowingStatus;
import com.nexusarchive.modules.borrowing.infra.mapper.BorrowingMapper;
import com.nexusarchive.service.DestructionValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 销毁校验服务实现
 * 
 * 实现要点：
 * 1. 检查档案是否存在在借记录（status = APPROVED）
 * 2. 检查档案是否被冻结（destructionHold = true）
 * 3. 检查档案状态是否为 FROZEN 或 HOLD
 * 4. 批量校验，返回所有不符合条件的档案信息
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DestructionValidationServiceImpl implements DestructionValidationService {
    
    private final BorrowingMapper borrowingMapper;
    private final ArchiveMapper archiveMapper;
    
    @Override
    public void validateDestructionEligibility(List<String> archiveIds, String fondsNo) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            throw new IllegalArgumentException("档案ID列表不能为空");
        }
        
        List<String> errors = new ArrayList<>();
        
        // 1. 查询档案列表
        List<Archive> archives = archiveMapper.selectBatchIds(archiveIds);
        if (archives.size() != archiveIds.size()) {
            errors.add("部分档案不存在");
        }
        
        // 2. 检查在借记录
        List<String> borrowedArchives = findBorrowedArchives(archiveIds);
        if (!borrowedArchives.isEmpty()) {
            for (String archiveId : borrowedArchives) {
                Archive archive = archives.stream()
                    .filter(a -> a.getId().equals(archiveId))
                    .findFirst()
                    .orElse(null);
                
                String archiveCode = archive != null ? archive.getArchiveCode() : archiveId;
                errors.add(String.format("档案 %s 正在借阅中，无法销毁", archiveCode));
            }
        }
        
        // 3. 检查冻结状态
        for (Archive archive : archives) {
            // 检查 destructionHold
            if (Boolean.TRUE.equals(archive.getDestructionHold())) {
                errors.add(String.format("档案 %s 处于冻结状态（原因：%s），无法销毁", 
                    archive.getArchiveCode(), archive.getHoldReason()));
            }
            
            // 检查 destructionStatus
            if ("FROZEN".equals(archive.getDestructionStatus()) || 
                "HOLD".equals(archive.getDestructionStatus())) {
                errors.add(String.format("档案 %s 处于 %s 状态，无法销毁", 
                    archive.getArchiveCode(), archive.getDestructionStatus()));
            }
            
            // 检查全宗号
            if (fondsNo != null && !fondsNo.equals(archive.getFondsNo())) {
                errors.add(String.format("档案 %s 不属于全宗 %s", 
                    archive.getArchiveCode(), fondsNo));
            }
        }
        
        // 4. 如果有错误，抛出异常
        if (!errors.isEmpty()) {
            String errorMessage = String.join("; ", errors);
            log.warn("销毁校验失败: {}", errorMessage);
            throw new DestructionNotAllowedException(errorMessage);
        }
        
        log.debug("销毁校验通过，档案数量: {}", archiveIds.size());
    }
    
    @Override
    public boolean isBorrowed(String archiveId, String fondsNo, Integer archiveYear) {
        // 查询是否存在 APPROVED 状态的借阅记录（在借中）
        LambdaQueryWrapper<Borrowing> queryWrapper = new LambdaQueryWrapper<Borrowing>()
                .eq(Borrowing::getArchiveId, archiveId)
                .in(Borrowing::getStatus, BorrowingStatus.borrowedCodes())
                .eq(Borrowing::getDeleted, 0);
        if (fondsNo != null && !fondsNo.isBlank()) {
            queryWrapper.eq(Borrowing::getFondsNo, fondsNo);
        }
        if (archiveYear != null) {
            queryWrapper.eq(Borrowing::getArchiveYear, archiveYear);
        }
        
        long count = borrowingMapper.selectCount(queryWrapper);
        return count > 0;
    }
    
    @Override
    public List<String> findBorrowedArchives(List<String> archiveIds) {
        if (archiveIds == null || archiveIds.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 查询所有在借的档案ID
        LambdaQueryWrapper<Borrowing> queryWrapper = new LambdaQueryWrapper<Borrowing>()
                .in(Borrowing::getArchiveId, archiveIds)
                .in(Borrowing::getStatus, BorrowingStatus.borrowedCodes())
                .eq(Borrowing::getDeleted, 0)
                .select(Borrowing::getArchiveId);
        
        List<Borrowing> borrowings = borrowingMapper.selectList(queryWrapper);
        
        // 提取档案ID并去重
        Set<String> borrowedArchiveIds = borrowings.stream()
                .map(Borrowing::getArchiveId)
                .collect(Collectors.toSet());
        
        return new ArrayList<>(borrowedArchiveIds);
    }
}
