// Input: FileHashDedupService, FileHashDedupScope, AuthTicketService
// Output: FileHashDedupServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.request.FileHashDedupScopeRequest;
import com.nexusarchive.entity.FileHashDedupScope;
import com.nexusarchive.mapper.FileHashDedupScopeMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.AuthTicketValidationService;
import com.nexusarchive.service.FileHashDedupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 文件哈希去重范围服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileHashDedupServiceImpl implements FileHashDedupService {
    
    private final FileHashDedupScopeMapper dedupScopeMapper;
    private final AuthTicketValidationService authTicketValidationService;
    private final AuditLogService auditLogService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String configureDedupScope(FileHashDedupScopeRequest request) {
        // 1. 查询是否已存在配置
        LambdaQueryWrapper<FileHashDedupScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileHashDedupScope::getFondsNo, request.getFondsNo())
               .eq(FileHashDedupScope::getDeleted, 0);
        
        FileHashDedupScope scope = dedupScopeMapper.selectOne(wrapper);
        
        if (scope == null) {
            // 创建新配置
            scope = new FileHashDedupScope();
            scope.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            scope.setFondsNo(request.getFondsNo());
            scope.setScopeType(request.getScopeType());
            scope.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            scope.setCreatedBy("SYSTEM");
            scope.setCreatedAt(LocalDateTime.now());
            scope.setUpdatedAt(LocalDateTime.now());
            scope.setDeleted(0);
            dedupScopeMapper.insert(scope);
        } else {
            // 更新现有配置
            scope.setScopeType(request.getScopeType());
            if (request.getEnabled() != null) {
                scope.setEnabled(request.getEnabled());
            }
            scope.setUpdatedAt(LocalDateTime.now());
            dedupScopeMapper.updateById(scope);
        }
        
        // 2. 记录审计日志
        auditLogService.log(
            "SYSTEM", "SYSTEM", "FILE_HASH_DEDUP_SCOPE_UPDATED",
            "FILE_HASH_DEDUP_SCOPE", scope.getId(), "SUCCESS",
            String.format("更新去重范围配置: fondsNo=%s, scopeType=%s", request.getFondsNo(), request.getScopeType()),
            "SYSTEM"
        );
        
        log.info("文件哈希去重范围配置已更新: scopeId={}, fondsNo={}, scopeType={}", 
            scope.getId(), request.getFondsNo(), request.getScopeType());
        
        return scope.getId();
    }
    
    @Override
    public boolean isDedupAllowed(String fondsNo, String fileHash, String targetFondsNo) {
        // 1. 获取去重范围配置
        String scopeType = getDedupScopeType(fondsNo);
        
        if (scopeType == null) {
            // 默认只允许同全宗去重
            return fondsNo.equals(targetFondsNo);
        }
        
        // 2. 根据范围类型判断
        switch (scopeType) {
            case "SAME_FONDS":
                // 仅允许同全宗去重
                return fondsNo.equals(targetFondsNo);
            case "AUTHORIZED":
                // 允许授权范围内的去重
                // TODO: 检查是否有跨全宗授权票据
                // 这里简化处理，假设有授权则允许
                if (fondsNo.equals(targetFondsNo)) {
                    return true;
                }
                // 检查跨全宗授权
                // return authTicketValidationService.hasCrossFondsAccess(fondsNo, targetFondsNo);
                return false; // 默认不允许跨全宗
            case "GLOBAL":
                // 允许全局去重（不推荐，可能泄露数据关联）
                return true;
            default:
                return false;
        }
    }
    
    @Override
    public String getDedupScopeType(String fondsNo) {
        LambdaQueryWrapper<FileHashDedupScope> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileHashDedupScope::getFondsNo, fondsNo)
               .eq(FileHashDedupScope::getEnabled, true)
               .eq(FileHashDedupScope::getDeleted, 0);
        
        FileHashDedupScope scope = dedupScopeMapper.selectOne(wrapper);
        
        return scope != null ? scope.getScopeType() : null;
    }
}

