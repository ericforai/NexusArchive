// Input: AuthTicketApprovalService, AuthTicketMapper, ObjectMapper
// Output: AuthTicketApprovalServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.ApprovalChain;
import com.nexusarchive.entity.AuthTicket;
import com.nexusarchive.mapper.AuthTicketMapper;
import com.nexusarchive.service.AuthTicketApprovalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 授权票据审批服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTicketApprovalServiceImpl implements AuthTicketApprovalService {
    
    private final AuthTicketMapper authTicketMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void firstApproval(String ticketId, String approverId, String approverName, 
                             String comment, boolean approved) {
        // 1. 查询授权票据
        AuthTicket ticket = authTicketMapper.selectById(ticketId);
        if (ticket == null || ticket.getDeleted() == 1) {
            throw new IllegalArgumentException("授权票据不存在: " + ticketId);
        }
        
        // 2. 状态校验：只有 PENDING 状态的票据可以进行第一审批
        if (!"PENDING".equals(ticket.getStatus())) {
            throw new IllegalStateException("只有待审批状态的票据可以进行第一审批，当前状态: " + ticket.getStatus());
        }
        
        // 3. 创建审批链
        ApprovalChain approvalChain = new ApprovalChain();
        ApprovalChain.ApprovalInfo firstApproval = new ApprovalChain.ApprovalInfo();
        firstApproval.setApproverId(approverId);
        firstApproval.setApproverName(approverName);
        firstApproval.setComment(comment);
        firstApproval.setApproved(approved);
        firstApproval.setTimestamp(LocalDateTime.now());
        approvalChain.setFirstApproval(firstApproval);
        
        // 4. 序列化审批链
        String approvalSnapshot;
        try {
            approvalSnapshot = objectMapper.writeValueAsString(approvalChain);
        } catch (Exception e) {
            log.error("序列化审批链失败", e);
            throw new RuntimeException("序列化审批链失败: " + e.getMessage(), e);
        }
        
        // 5. 更新票据状态
        if (approved) {
            ticket.setStatus("FIRST_APPROVED");
        } else {
            ticket.setStatus("REJECTED");
        }
        ticket.setApprovalSnapshot(approvalSnapshot);
        ticket.setLastModifiedTime(LocalDateTime.now());
        
        // 6. 保存到数据库
        authTicketMapper.updateById(ticket);
        
        log.info("第一审批完成: ticketId={}, approverId={}, approved={}", 
                ticketId, approverId, approved);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void secondApproval(String ticketId, String approverId, String approverName, 
                              String comment, boolean approved) {
        // 1. 查询授权票据
        AuthTicket ticket = authTicketMapper.selectById(ticketId);
        if (ticket == null || ticket.getDeleted() == 1) {
            throw new IllegalArgumentException("授权票据不存在: " + ticketId);
        }
        
        // 2. 状态校验：只有 FIRST_APPROVED 状态的票据可以进行第二审批
        if (!"FIRST_APPROVED".equals(ticket.getStatus())) {
            throw new IllegalStateException("只有第一审批通过的票据可以进行第二审批，当前状态: " + ticket.getStatus());
        }
        
        // 3. 反序列化现有审批链
        ApprovalChain approvalChain;
        try {
            if (ticket.getApprovalSnapshot() != null && !ticket.getApprovalSnapshot().isEmpty()) {
                approvalChain = objectMapper.readValue(ticket.getApprovalSnapshot(), ApprovalChain.class);
            } else {
                approvalChain = new ApprovalChain();
            }
        } catch (Exception e) {
            log.error("反序列化审批链失败", e);
            throw new RuntimeException("反序列化审批链失败: " + e.getMessage(), e);
        }
        
        // 4. 添加第二审批信息
        ApprovalChain.ApprovalInfo secondApproval = new ApprovalChain.ApprovalInfo();
        secondApproval.setApproverId(approverId);
        secondApproval.setApproverName(approverName);
        secondApproval.setComment(comment);
        secondApproval.setApproved(approved);
        secondApproval.setTimestamp(LocalDateTime.now());
        approvalChain.setSecondApproval(secondApproval);
        
        // 5. 序列化审批链
        String approvalSnapshot;
        try {
            approvalSnapshot = objectMapper.writeValueAsString(approvalChain);
        } catch (Exception e) {
            log.error("序列化审批链失败", e);
            throw new RuntimeException("序列化审批链失败: " + e.getMessage(), e);
        }
        
        // 6. 更新票据状态
        if (approved) {
            ticket.setStatus("APPROVED");
        } else {
            ticket.setStatus("REJECTED");
        }
        ticket.setApprovalSnapshot(approvalSnapshot);
        ticket.setLastModifiedTime(LocalDateTime.now());
        
        // 7. 保存到数据库
        authTicketMapper.updateById(ticket);
        
        log.info("第二审批完成: ticketId={}, approverId={}, approved={}", 
                ticketId, approverId, approved);
    }
    
    @Override
    public ApprovalChain getApprovalChain(String ticketId) {
        // 1. 查询授权票据
        AuthTicket ticket = authTicketMapper.selectById(ticketId);
        if (ticket == null || ticket.getDeleted() == 1) {
            throw new IllegalArgumentException("授权票据不存在: " + ticketId);
        }
        
        // 2. 反序列化审批链
        if (ticket.getApprovalSnapshot() == null || ticket.getApprovalSnapshot().isEmpty()) {
            return null;
        }
        
        try {
            return objectMapper.readValue(ticket.getApprovalSnapshot(), ApprovalChain.class);
        } catch (Exception e) {
            log.error("反序列化审批链失败: ticketId={}", ticketId, e);
            return null;
        }
    }
}





