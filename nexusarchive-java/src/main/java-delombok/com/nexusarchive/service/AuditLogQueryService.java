// Input: MyBatis-Plus、Lombok、Spring Framework、Java 标准库、等
// Output: AuditLogQueryService 类
// Pos: 业务服务层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.nexusarchive.entity.SysAuditLog;
import com.nexusarchive.mapper.SysAuditLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AuditLogQueryService {

    private final SysAuditLogMapper auditLogMapper;

    public Page<SysAuditLog> query(int page, int limit, String userId, String resourceType, String action) {
        LambdaQueryWrapper<SysAuditLog> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(userId)) {
            wrapper.eq(SysAuditLog::getUserId, userId);
        }
        if (StringUtils.hasText(resourceType)) {
            wrapper.eq(SysAuditLog::getResourceType, resourceType);
        }
        if (StringUtils.hasText(action)) {
            wrapper.eq(SysAuditLog::getAction, action);
        }
        wrapper.orderByDesc(SysAuditLog::getCreatedTime);
        Page<SysAuditLog> p = new Page<>(page, limit);
        return auditLogMapper.selectPage(p, wrapper);
    }
}
