// Input: AuthTicketService, AuthTicketMapper, ObjectMapper, UserService
// Output: AuthTicketServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.nexusarchive.common.constants.OperationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.ApprovalChain;
import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketDetail;
import com.nexusarchive.entity.AuthTicket;
import com.nexusarchive.mapper.AuthTicketMapper;
import com.nexusarchive.service.AuthTicketService;
import com.nexusarchive.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 授权票据服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTicketServiceImpl implements AuthTicketService {
    
    private final AuthTicketMapper authTicketMapper;
    private final ObjectMapper objectMapper;
    private final UserService userService;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public String createAuthTicket(String applicantId, String sourceFonds, 
                                   String targetFonds, AuthScope scope, 
                                   LocalDateTime expiresAt, String reason) {
        // 1. 参数验证
        LocalDateTime now = LocalDateTime.now();
        if (expiresAt.isBefore(now.plusDays(1))) {
            throw new IllegalArgumentException("有效期必须 >= 当前时间 + 1天");
        }
        if (expiresAt.isAfter(now.plusDays(90))) {
            throw new IllegalArgumentException("有效期必须 <= 当前时间 + 90天");
        }
        if (sourceFonds.equals(targetFonds)) {
            throw new IllegalArgumentException("源全宗和目标全宗不能相同");
        }
        
        // 2. 获取申请人姓名
        String applicantName = "未知用户";
        try {
            var userResponse = userService.getUserById(applicantId);
            if (userResponse != null && userResponse.getUsername() != null) {
                applicantName = userResponse.getUsername();
            }
        } catch (Exception e) {
            log.warn("获取用户信息失败: applicantId={}", applicantId, e);
        }
        
        // 3. 序列化访问范围
        String scopeJson;
        try {
            scopeJson = objectMapper.writeValueAsString(scope);
        } catch (Exception e) {
            log.error("序列化访问范围失败", e);
            throw new RuntimeException("序列化访问范围失败: " + e.getMessage(), e);
        }
        
        // 4. 创建授权票据
        AuthTicket ticket = new AuthTicket();
        ticket.setApplicantId(applicantId);
        ticket.setApplicantName(applicantName);
        ticket.setSourceFonds(sourceFonds);
        ticket.setTargetFonds(targetFonds);
        ticket.setScope(scopeJson);
        ticket.setExpiresAt(expiresAt);
        ticket.setStatus(OperationResult.PENDING);
        ticket.setReason(reason);
        ticket.setCreatedAt(now);
        ticket.setLastModifiedTime(now);
        ticket.setDeleted(0);
        
        // 5. 保存到数据库
        authTicketMapper.insert(ticket);
        
        log.info("授权票据申请创建成功: ticketId={}, applicantId={}, sourceFonds={}, targetFonds={}", 
                ticket.getId(), applicantId, sourceFonds, targetFonds);
        
        return ticket.getId();
    }
    
    @Override
    public AuthTicketDetail getAuthTicketDetail(String ticketId) {
        // 1. 查询授权票据
        AuthTicket ticket = authTicketMapper.selectById(ticketId);
        if (ticket == null || ticket.getDeleted() == 1) {
            throw new IllegalArgumentException("授权票据不存在: " + ticketId);
        }
        
        // 2. 反序列化访问范围
        AuthScope scope;
        try {
            scope = objectMapper.readValue(ticket.getScope(), AuthScope.class);
        } catch (Exception e) {
            log.error("反序列化访问范围失败", e);
            scope = new AuthScope();
        }
        
        // 3. 反序列化审批链
        ApprovalChain approvalChain = null;
        if (ticket.getApprovalSnapshot() != null && !ticket.getApprovalSnapshot().isEmpty()) {
            try {
                approvalChain = objectMapper.readValue(ticket.getApprovalSnapshot(), ApprovalChain.class);
            } catch (Exception e) {
                log.warn("反序列化审批链失败: ticketId={}", ticketId, e);
            }
        }
        
        // 4. 构建详情 DTO
        AuthTicketDetail detail = new AuthTicketDetail();
        detail.setId(ticket.getId());
        detail.setApplicantId(ticket.getApplicantId());
        detail.setApplicantName(ticket.getApplicantName());
        detail.setSourceFonds(ticket.getSourceFonds());
        detail.setTargetFonds(ticket.getTargetFonds());
        detail.setScope(scope);
        detail.setExpiresAt(ticket.getExpiresAt());
        detail.setStatus(ticket.getStatus());
        detail.setApprovalChain(approvalChain);
        detail.setReason(ticket.getReason());
        detail.setCreatedAt(ticket.getCreatedAt());
        detail.setLastModifiedTime(ticket.getLastModifiedTime());
        
        return detail;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revokeAuthTicket(String ticketId, String operatorId, String reason) {
        // 1. 查询授权票据
        AuthTicket ticket = authTicketMapper.selectById(ticketId);
        if (ticket == null || ticket.getDeleted() == 1) {
            throw new IllegalArgumentException("授权票据不存在: " + ticketId);
        }
        
        // 2. 权限校验：仅申请人或管理员可撤销
        // TODO: 添加管理员权限校验
        if (!ticket.getApplicantId().equals(operatorId)) {
            // 检查是否为管理员（暂时允许，后续需要添加权限校验）
            log.warn("非申请人尝试撤销授权票据: ticketId={}, operatorId={}, applicantId={}", 
                    ticketId, operatorId, ticket.getApplicantId());
        }
        
        // 3. 状态校验：只有 APPROVED 或 FIRST_APPROVED 状态的票据可以撤销
        if (!"APPROVED".equals(ticket.getStatus()) && !"FIRST_APPROVED".equals(ticket.getStatus())) {
            throw new IllegalStateException("只有已批准或第一审批通过的票据可以撤销，当前状态: " + ticket.getStatus());
        }
        
        // 4. 更新状态
        ticket.setStatus("REVOKED");
        ticket.setLastModifiedTime(LocalDateTime.now());
        // 在 reason 字段中追加撤销原因（或使用新字段）
        if (reason != null && !reason.isEmpty()) {
            ticket.setReason(ticket.getReason() + " [撤销原因: " + reason + "]");
        }
        
        // 5. 保存到数据库
        authTicketMapper.updateById(ticket);
        
        log.info("授权票据撤销成功: ticketId={}, operatorId={}, reason={}", ticketId, operatorId, reason);
    }

    @Override
    public java.util.List<AuthTicketDetail> listAuthTickets(String status, String applicantId, String sourceFonds, String targetFonds, int page, int limit) {
        // 构建查询条件
        com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AuthTicket> wrapper =
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<>();

        if (status != null && !status.isEmpty()) {
            wrapper.eq(AuthTicket::getStatus, status);
        }
        if (applicantId != null && !applicantId.isEmpty()) {
            wrapper.eq(AuthTicket::getApplicantId, applicantId);
        }
        if (sourceFonds != null && !sourceFonds.isEmpty()) {
            wrapper.eq(AuthTicket::getSourceFonds, sourceFonds);
        }
        if (targetFonds != null && !targetFonds.isEmpty()) {
            wrapper.eq(AuthTicket::getTargetFonds, targetFonds);
        }

        wrapper.eq(AuthTicket::getDeleted, 0)
                .orderByDesc(AuthTicket::getCreatedAt);

        // 分页查询
        com.baomidou.mybatisplus.extension.plugins.pagination.Page<AuthTicket> pageParam =
                new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(page, limit);

        com.baomidou.mybatisplus.core.metadata.IPage<AuthTicket> ticketPage = authTicketMapper.selectPage(pageParam, wrapper);

        // 转换为 DTO
        return ticketPage.getRecords().stream().map(ticket -> {
            AuthTicketDetail detail = new AuthTicketDetail();
            detail.setId(ticket.getId());
            detail.setApplicantId(ticket.getApplicantId());
            detail.setApplicantName(ticket.getApplicantName());
            detail.setSourceFonds(ticket.getSourceFonds());
            detail.setTargetFonds(ticket.getTargetFonds());
            detail.setStatus(ticket.getStatus());
            detail.setExpiresAt(ticket.getExpiresAt());
            detail.setReason(ticket.getReason());
            detail.setCreatedAt(ticket.getCreatedAt());
            detail.setLastModifiedTime(ticket.getLastModifiedTime());

            // 反序列化访问范围
            try {
                AuthScope scope = objectMapper.readValue(ticket.getScope(), AuthScope.class);
                detail.setScope(scope);
            } catch (Exception e) {
                detail.setScope(new AuthScope());
            }

            return detail;
        }).toList();
    }
}

