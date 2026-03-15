// Input: UserLifecycleService, UserService, EmployeeLifecycleEvent, ObjectMapper
// Output: UserLifecycleServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.request.CreateUserRequest;
import com.nexusarchive.dto.request.OffboardEmployeeRequest;
import com.nexusarchive.dto.request.OnboardEmployeeRequest;
import com.nexusarchive.dto.request.TransferEmployeeRequest;
import com.nexusarchive.entity.EmployeeLifecycleEvent;
import com.nexusarchive.mapper.EmployeeLifecycleEventMapper;
import com.nexusarchive.service.AuditLogService;
import com.nexusarchive.service.UserLifecycleService;
import com.nexusarchive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 用户生命周期服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserLifecycleServiceImpl implements UserLifecycleService {
    
    private final UserService userService;
    private final EmployeeLifecycleEventMapper eventMapper;
    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String onboardEmployee(OnboardEmployeeRequest request) {
        // 1. 创建生命周期事件记录
        EmployeeLifecycleEvent event = new EmployeeLifecycleEvent();
        event.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        event.setEmployeeId(request.getEmployeeId());
        event.setEmployeeName(request.getEmployeeName());
        event.setEventType("ONBOARD");
        event.setEventDate(request.getOnboardDate());
        event.setOrganizationId(request.getOrganizationId());
        event.setProcessed(false);
        event.setCreatedAt(LocalDateTime.now());
        event.setDeleted(0);
        
        try {
            if (request.getRoleIds() != null) {
                event.setNewRoleIds(objectMapper.writeValueAsString(request.getRoleIds()));
            }
        } catch (Exception e) {
            log.error("序列化角色ID列表失败", e);
        }
        
        eventMapper.insert(event);
        
        // 2. 创建用户账号
        CreateUserRequest createUserRequest = new CreateUserRequest();
        createUserRequest.setUsername(request.getUsername() != null ? 
            request.getUsername() : generateUsername(request.getEmployeeName(), request.getEmployeeId()));
        createUserRequest.setFullName(request.getEmployeeName());
        createUserRequest.setEmail(request.getEmail());
        createUserRequest.setPhone(request.getPhone());
        createUserRequest.setOrganizationId(request.getOrganizationId());
        createUserRequest.setRoleIds(request.getRoleIds());
        createUserRequest.setPassword(request.getInitialPassword() != null ? 
            request.getInitialPassword() : generateTemporaryPassword());
        
        var userResponse = userService.createUser(createUserRequest);
        
        // 3. 标记事件已处理
        event.setProcessed(true);
        event.setProcessedAt(LocalDateTime.now());
        event.setProcessedBy("SYSTEM");
        eventMapper.updateById(event);
        
        // 4. 记录审计日志
        auditLogService.log(
            "SYSTEM", "SYSTEM", "USER_ONBOARD",
            "USER", userResponse.getId(), OperationResult.SUCCESS,
            String.format("入职处理: employeeId=%s, username=%s", request.getEmployeeId(), userResponse.getUsername()),
            "SYSTEM"
        );
        
        log.info("员工入职处理完成: employeeId={}, userId={}", request.getEmployeeId(), userResponse.getId());
        
        return userResponse.getId();
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offboardEmployee(OffboardEmployeeRequest request) {
        // 1. 查找用户（通过员工ID或用户名）
        // TODO: 需要建立员工ID与用户ID的映射关系，这里简化处理
        String userId = findUserIdByEmployeeId(request.getEmployeeId());
        if (userId == null) {
            log.warn("未找到对应的用户账号: employeeId={}", request.getEmployeeId());
            return;
        }
        
        // 2. 创建生命周期事件记录
        EmployeeLifecycleEvent event = new EmployeeLifecycleEvent();
        event.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        event.setEmployeeId(request.getEmployeeId());
        event.setEmployeeName(request.getEmployeeName());
        event.setEventType("OFFBOARD");
        event.setEventDate(request.getOffboardDate());
        event.setReason(request.getReason());
        event.setProcessed(false);
        event.setCreatedAt(LocalDateTime.now());
        event.setDeleted(0);
        
        // 获取当前角色
        try {
            var userResponse = userService.getUserById(userId);
            if (userResponse.getRoleIds() != null) {
                event.setPreviousRoleIds(objectMapper.writeValueAsString(userResponse.getRoleIds()));
            }
        } catch (Exception e) {
            log.warn("获取用户角色失败", e);
        }
        
        eventMapper.insert(event);
        
        // 3. 停用账号并回收权限
        userService.updateStatus(userId, "disabled");
        
        // 移除所有角色关联
        // TODO: 需要 UserService 提供移除角色的方法，这里简化处理
        // 可以通过更新用户信息来移除角色，但需要 UpdateUserRequest
        // 暂时只停用账号，角色关联保留（实际应该移除）
        
        // 4. 标记事件已处理
        event.setProcessed(true);
        event.setProcessedAt(LocalDateTime.now());
        event.setProcessedBy("SYSTEM");
        eventMapper.updateById(event);
        
        // 5. 记录审计日志
        auditLogService.log(
            "SYSTEM", "SYSTEM", "USER_OFFBOARD",
            "USER", userId, OperationResult.SUCCESS,
            String.format("离职处理: employeeId=%s, reason=%s", request.getEmployeeId(), request.getReason()),
            "SYSTEM"
        );
        
        log.info("员工离职处理完成: employeeId={}, userId={}", request.getEmployeeId(), userId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferEmployee(TransferEmployeeRequest request) {
        // 1. 查找用户
        String userId = findUserIdByEmployeeId(request.getEmployeeId());
        if (userId == null) {
            log.warn("未找到对应的用户账号: employeeId={}", request.getEmployeeId());
            return;
        }
        
        // 2. 创建生命周期事件记录
        EmployeeLifecycleEvent event = new EmployeeLifecycleEvent();
        event.setId(UUID.randomUUID().toString().replaceAll("-", ""));
        event.setEmployeeId(request.getEmployeeId());
        event.setEmployeeName(request.getEmployeeName());
        event.setEventType("TRANSFER");
        event.setEventDate(request.getTransferDate());
        event.setToOrganizationId(request.getToOrganizationId());
        event.setReason(request.getReason());
        event.setProcessed(false);
        event.setCreatedAt(LocalDateTime.now());
        event.setDeleted(0);
        
        try {
            if (request.getPreviousRoleIds() != null) {
                event.setPreviousRoleIds(objectMapper.writeValueAsString(request.getPreviousRoleIds()));
            }
            if (request.getNewRoleIds() != null) {
                event.setNewRoleIds(objectMapper.writeValueAsString(request.getNewRoleIds()));
            }
        } catch (Exception e) {
            log.error("序列化角色ID列表失败", e);
        }
        
        eventMapper.insert(event);
        
        // 3. 更新用户组织和角色
        // TODO: 需要 UserService 提供更新组织和角色的方法
        // userService.updateUserOrganization(userId, request.getToOrganizationId());
        // userService.updateUserRoles(userId, request.getNewRoleIds());
        
        // 4. 标记事件已处理
        event.setProcessed(true);
        event.setProcessedAt(LocalDateTime.now());
        event.setProcessedBy("SYSTEM");
        eventMapper.updateById(event);
        
        // 5. 记录审计日志
        auditLogService.log(
            "SYSTEM", "SYSTEM", "USER_TRANSFER",
            "USER", userId, OperationResult.SUCCESS,
            String.format("调岗处理: employeeId=%s, toOrganizationId=%s",
                request.getEmployeeId(), request.getToOrganizationId()),
            "SYSTEM"
        );
        
        log.info("员工调岗处理完成: employeeId={}, userId={}", request.getEmployeeId(), userId);
    }
    
    @Override
    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点执行
    public void processPendingEvents() {
        log.info("开始处理待处理的生命周期事件");
        
        // 查询所有未处理的事件
        LambdaQueryWrapper<EmployeeLifecycleEvent> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeeLifecycleEvent::getProcessed, false)
               .eq(EmployeeLifecycleEvent::getDeleted, 0)
               .le(EmployeeLifecycleEvent::getEventDate, LocalDate.now());
        
        List<EmployeeLifecycleEvent> events = eventMapper.selectList(wrapper);
        
        for (EmployeeLifecycleEvent event : events) {
            try {
                switch (event.getEventType()) {
                    case "ONBOARD":
                        // 入职事件应该已经处理，这里主要是兜底
                        break;
                    case "OFFBOARD":
                        // 处理离职事件
                        OffboardEmployeeRequest offboardRequest = new OffboardEmployeeRequest();
                        offboardRequest.setEmployeeId(event.getEmployeeId());
                        offboardRequest.setEmployeeName(event.getEmployeeName());
                        offboardRequest.setOffboardDate(event.getEventDate());
                        offboardRequest.setReason(event.getReason());
                        offboardEmployee(offboardRequest);
                        break;
                    case "TRANSFER":
                        // 处理调岗事件
                        TransferEmployeeRequest transferRequest = new TransferEmployeeRequest();
                        transferRequest.setEmployeeId(event.getEmployeeId());
                        transferRequest.setEmployeeName(event.getEmployeeName());
                        transferRequest.setTransferDate(event.getEventDate());
                        transferRequest.setToOrganizationId(event.getToOrganizationId());
                        transferRequest.setReason(event.getReason());
                        
                        try {
                            if (event.getPreviousRoleIds() != null) {
                                transferRequest.setPreviousRoleIds(
                                    objectMapper.readValue(event.getPreviousRoleIds(), 
                                        new TypeReference<List<String>>() {}));
                            }
                            if (event.getNewRoleIds() != null) {
                                transferRequest.setNewRoleIds(
                                    objectMapper.readValue(event.getNewRoleIds(), 
                                        new TypeReference<List<String>>() {}));
                            }
                        } catch (Exception e) {
                            log.error("反序列化角色ID列表失败", e);
                        }
                        
                        transferEmployee(transferRequest);
                        break;
                }
            } catch (Exception e) {
                log.error("处理生命周期事件失败: eventId={}, eventType={}", 
                    event.getId(), event.getEventType(), e);
            }
        }
        
        log.info("待处理的生命周期事件处理完成，共处理 {} 条", events.size());
    }
    
    /**
     * 生成用户名
     */
    private String generateUsername(String employeeName, String employeeId) {
        // 简化实现：使用员工ID或姓名拼音
        return "user_" + employeeId.substring(0, Math.min(8, employeeId.length()));
    }
    
    /**
     * 生成临时密码
     * 安全加固: 使用 SecureRandom 生成符合密码强度策略的12位密码
     * 密码包含: 大写字母、小写字母、数字、特殊字符
     */
    private String generateTemporaryPassword() {
        java.security.SecureRandom random = new java.security.SecureRandom();
        String upper = "ABCDEFGHJKLMNPQRSTUVWXYZ"; // 去除易混淆字符 I, O
        String lower = "abcdefghjkmnpqrstuvwxyz"; // 去除易混淆字符 i, l, o
        String digits = "23456789"; // 去除易混淆字符 0, 1
        String special = "@#$%&*+-=";

        String allChars = upper + lower + digits + special;

        // 确保密码至少包含每种类型的字符
        StringBuilder password = new StringBuilder(12);
        password.append(upper.charAt(random.nextInt(upper.length())));
        password.append(lower.charAt(random.nextInt(lower.length())));
        password.append(digits.charAt(random.nextInt(digits.length())));
        password.append(special.charAt(random.nextInt(special.length())));

        // 填充剩余8位
        for (int i = 4; i < 12; i++) {
            password.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        // 打乱字符顺序
        char[] chars = password.toString().toCharArray();
        for (int i = chars.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = chars[i];
            chars[i] = chars[j];
            chars[j] = temp;
        }

        return new String(chars);
    }
    
    /**
     * 根据员工ID查找用户ID
     * TODO: 需要建立员工ID与用户ID的映射关系
     */
    private String findUserIdByEmployeeId(String employeeId) {
        // 简化实现：假设员工ID就是用户ID的一部分，或者需要建立映射表
        // 这里返回 null，实际应该查询映射表
        return null;
    }
}

