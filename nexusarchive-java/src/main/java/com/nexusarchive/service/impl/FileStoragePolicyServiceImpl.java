// Input: FileStoragePolicyService, FileStoragePolicy, FileStorageService
// Output: FileStoragePolicyServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.nexusarchive.dto.request.FileStoragePolicyRequest;
import com.nexusarchive.entity.FileStoragePolicy;
import com.nexusarchive.mapper.FileStoragePolicyMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.FileStoragePolicyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 文件存储策略服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FileStoragePolicyServiceImpl implements FileStoragePolicyService {
    
    private final FileStoragePolicyMapper policyMapper;
    private final AuditLogService auditLogService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createOrUpdatePolicy(FileStoragePolicyRequest request) {
        // 1. 查询是否已存在策略
        LambdaQueryWrapper<FileStoragePolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileStoragePolicy::getFondsNo, request.getFondsNo())
               .eq(FileStoragePolicy::getPolicyType, request.getPolicyType())
               .eq(FileStoragePolicy::getDeleted, 0);
        
        FileStoragePolicy policy = policyMapper.selectOne(wrapper);
        
        if (policy == null) {
            // 创建新策略
            policy = new FileStoragePolicy();
            policy.setId(UUID.randomUUID().toString().replaceAll("-", ""));
            policy.setFondsNo(request.getFondsNo());
            policy.setPolicyType(request.getPolicyType());
            policy.setRetentionDays(request.getRetentionDays());
            policy.setImmutableUntil(request.getImmutableUntil());
            policy.setEnabled(request.getEnabled() != null ? request.getEnabled() : true);
            policy.setCreatedBy("SYSTEM");
            policy.setCreatedAt(LocalDateTime.now());
            policy.setUpdatedAt(LocalDateTime.now());
            policy.setDeleted(0);
            policyMapper.insert(policy);
        } else {
            // 更新现有策略
            policy.setRetentionDays(request.getRetentionDays());
            policy.setImmutableUntil(request.getImmutableUntil());
            if (request.getEnabled() != null) {
                policy.setEnabled(request.getEnabled());
            }
            policy.setUpdatedAt(LocalDateTime.now());
            policyMapper.updateById(policy);
        }
        
        // 2. 记录审计日志
        auditLogService.log(
            "SYSTEM", "SYSTEM", "FILE_STORAGE_POLICY_UPDATED",
            "FILE_STORAGE_POLICY", policy.getId(), "SUCCESS",
            String.format("更新存储策略: fondsNo=%s, type=%s", request.getFondsNo(), request.getPolicyType()),
            "SYSTEM"
        );
        
        log.info("文件存储策略已更新: policyId={}, fondsNo={}, type={}", 
            policy.getId(), request.getFondsNo(), request.getPolicyType());
        
        return policy.getId();
    }
    
    @Override
    public List<FileStoragePolicy> getPoliciesByFonds(String fondsNo) {
        LambdaQueryWrapper<FileStoragePolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileStoragePolicy::getFondsNo, fondsNo)
               .eq(FileStoragePolicy::getDeleted, 0)
               .orderByDesc(FileStoragePolicy::getCreatedAt);
        
        return policyMapper.selectList(wrapper);
    }
    
    @Override
    public boolean isFileImmutable(String fondsNo, String filePath) {
        // 查询不可变策略
        LambdaQueryWrapper<FileStoragePolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileStoragePolicy::getFondsNo, fondsNo)
               .eq(FileStoragePolicy::getPolicyType, "IMMUTABLE")
               .eq(FileStoragePolicy::getEnabled, true)
               .eq(FileStoragePolicy::getDeleted, 0);
        
        FileStoragePolicy policy = policyMapper.selectOne(wrapper);
        
        if (policy == null) {
            return false;
        }
        
        // 检查是否在不可变期内
        if (policy.getImmutableUntil() != null) {
            return LocalDate.now().isBefore(policy.getImmutableUntil()) || 
                   LocalDate.now().isEqual(policy.getImmutableUntil());
        }
        
        // 如果没有设置截止日期，默认永久不可变
        return true;
    }
    
    @Override
    public boolean isFileInRetention(String fondsNo, String filePath) {
        // 查询保留策略
        LambdaQueryWrapper<FileStoragePolicy> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(FileStoragePolicy::getFondsNo, fondsNo)
               .eq(FileStoragePolicy::getPolicyType, "RETENTION")
               .eq(FileStoragePolicy::getEnabled, true)
               .eq(FileStoragePolicy::getDeleted, 0);
        
        FileStoragePolicy policy = policyMapper.selectOne(wrapper);
        
        if (policy == null) {
            return false;
        }
        
        // 如果保留天数为 null，表示永久保留
        if (policy.getRetentionDays() == null) {
            return true;
        }
        
        // TODO: 需要获取文件的创建时间，计算是否在保留期内
        // 这里简化处理，假设文件在保留期内
        return true;
    }
}

