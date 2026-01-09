// Input: Spring Framework, Lombok, MyBatis-Plus, PathSecurityUtils
// Output: FolderMonitorServiceImpl (文件夹监控服务实现)
// Pos: Service Layer

package com.nexusarchive.service.impl;

import com.nexusarchive.common.exception.BusinessException;
import com.nexusarchive.dto.scan.FolderMonitorRequest;
import com.nexusarchive.entity.ScanFolderMonitor;
import com.nexusarchive.mapper.ScanFolderMonitorMapper;
import com.nexusarchive.service.FolderMonitorService;
import com.nexusarchive.util.PathSecurityUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 文件夹监控服务实现
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FolderMonitorServiceImpl implements FolderMonitorService {

    private final ScanFolderMonitorMapper folderMonitorMapper;
    private final PathSecurityUtils pathSecurityUtils;

    @Value("${archive.monitor.root.path:./data/monitored}")
    private String monitorRootPath;

    @Override
    public List<ScanFolderMonitor> findByUserId(String userId) {
        return folderMonitorMapper.findByUserId(userId);
    }

    /**
     * 验证文件夹路径安全性
     * 防止路径遍历攻击，确保路径在允许的监控根目录内
     *
     * @param folderPath 用户提供的文件夹路径
     * @throws BusinessException 如果路径不安全或为空
     */
    private void validateFolderPath(String folderPath) {
        if (folderPath == null || folderPath.isBlank()) {
            throw new BusinessException("文件夹路径不能为空");
        }

        // 使用 PathSecurityUtils 进行路径验证
        try {
            Path validatedPath = pathSecurityUtils.validateAndNormalize(folderPath, monitorRootPath);
            log.debug("路径验证通过: {} -> {}", folderPath, validatedPath);
        } catch (SecurityException e) {
            log.warn("路径安全验证失败: {}", folderPath);
            throw new BusinessException("文件夹路径包含非法字符或尝试访问非法目录");
        }
    }

    /**
     * 验证用户ID
     *
     * @param userId 用户ID
     * @throws BusinessException 如果用户ID无效
     */
    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new BusinessException("用户身份信息无效");
        }
    }

    @Override
    @Transactional
    public ScanFolderMonitor addMonitor(String userId, FolderMonitorRequest request) {
        // 验证用户ID
        validateUserId(userId);

        // 验证文件夹路径安全性
        validateFolderPath(request.getFolderPath());

        // 如果提供了移动路径，也要验证
        if (request.getMoveToPath() != null && !request.getMoveToPath().isBlank()) {
            validateFolderPath(request.getMoveToPath());
        }

        // 检查是否已存在相同路径的监控
        ScanFolderMonitor existing = folderMonitorMapper.findByFolderPath(request.getFolderPath());
        if (existing != null) {
            throw new BusinessException("该文件夹路径已被监控");
        }

        ScanFolderMonitor monitor = new ScanFolderMonitor();
        monitor.setUserId(userId);
        monitor.setFolderPath(request.getFolderPath());
        monitor.setFileFilter(request.getFileFilter());
        monitor.setAutoDelete(request.getAutoDelete());
        monitor.setMoveToPath(request.getMoveToPath());
        monitor.setIsActive(true);
        monitor.setCreatedAt(LocalDateTime.now());
        monitor.setUpdatedAt(LocalDateTime.now());

        folderMonitorMapper.insert(monitor);
        log.info("添加文件夹监控: userId={}, path={}", userId, request.getFolderPath());
        return monitor;
    }

    @Override
    @Transactional
    public ScanFolderMonitor updateMonitor(Long id, String userId, FolderMonitorRequest request) {
        // 验证用户ID
        validateUserId(userId);

        ScanFolderMonitor monitor = folderMonitorMapper.selectById(id);
        if (monitor == null) {
            throw new BusinessException("监控配置不存在");
        }

        // 权限检查
        if (!monitor.getUserId().equals(userId)) {
            log.warn("未授权的访问尝试: userId={}, monitorOwnerId={}", userId, monitor.getUserId());
            throw new BusinessException("无权操作此监控配置");
        }

        // 验证文件夹路径安全性
        validateFolderPath(request.getFolderPath());

        // 如果提供了移动路径，也要验证
        if (request.getMoveToPath() != null && !request.getMoveToPath().isBlank()) {
            validateFolderPath(request.getMoveToPath());
        }

        monitor.setFolderPath(request.getFolderPath());
        monitor.setFileFilter(request.getFileFilter());
        monitor.setAutoDelete(request.getAutoDelete());
        monitor.setMoveToPath(request.getMoveToPath());
        monitor.setUpdatedAt(LocalDateTime.now());

        folderMonitorMapper.updateById(monitor);
        log.info("更新文件夹监控: id={}, userId={}", id, userId);
        return monitor;
    }

    @Override
    @Transactional
    public void deleteMonitor(Long id, String userId) {
        // 验证用户ID
        validateUserId(userId);

        ScanFolderMonitor monitor = folderMonitorMapper.selectById(id);
        if (monitor == null) {
            throw new BusinessException("监控配置不存在");
        }

        // 权限检查
        if (!monitor.getUserId().equals(userId)) {
            log.warn("未授权的访问尝试: userId={}, monitorOwnerId={}", userId, monitor.getUserId());
            throw new BusinessException("无权操作此监控配置");
        }

        folderMonitorMapper.deleteById(id);
        log.info("删除文件夹监控: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public ScanFolderMonitor toggleMonitor(Long id, String userId) {
        // 验证用户ID
        validateUserId(userId);

        ScanFolderMonitor monitor = folderMonitorMapper.selectById(id);
        if (monitor == null) {
            throw new BusinessException("监控配置不存在");
        }

        // 权限检查
        if (!monitor.getUserId().equals(userId)) {
            log.warn("未授权的访问尝试: userId={}, monitorOwnerId={}", userId, monitor.getUserId());
            throw new BusinessException("无权操作此监控配置");
        }

        monitor.setIsActive(!monitor.getIsActive());
        monitor.setUpdatedAt(LocalDateTime.now());
        folderMonitorMapper.updateById(monitor);

        log.info("切换文件夹监控状态: id={}, isActive={}", id, monitor.getIsActive());
        return monitor;
    }
}
