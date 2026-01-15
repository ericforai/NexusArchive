// Input: AuthTicketValidationService, AuthTicketMapper, ObjectMapper
// Output: AuthTicketValidationServiceImpl 类
// Pos: 业务服务实现层
// 一旦我被更新，务必更新我的开头注释，以及所属的文件夹的 md。

package com.nexusarchive.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nexusarchive.dto.AuthScope;
import com.nexusarchive.dto.AuthTicketValidationResult;
import com.nexusarchive.entity.AuthTicket;
import com.nexusarchive.mapper.AuthTicketMapper;
import com.nexusarchive.service.AuthTicketValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 授权票据验证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthTicketValidationServiceImpl implements AuthTicketValidationService {
    
    private final AuthTicketMapper authTicketMapper;
    private final ObjectMapper objectMapper;
    
    @Override
    public AuthTicketValidationResult validateTicket(String ticketId, String targetFonds, 
                                                     AuthScope accessScope) {
        // 1. 查询授权票据
        AuthTicket ticket = authTicketMapper.selectById(ticketId);
        if (ticket == null || ticket.getDeleted() == 1) {
            return AuthTicketValidationResult.invalid("授权票据不存在: " + ticketId);
        }
        
        // 2. 检查状态：必须是 APPROVED
        if (!"APPROVED".equals(ticket.getStatus())) {
            return AuthTicketValidationResult.invalid(
                "授权票据状态无效，当前状态: " + ticket.getStatus() + "，需要状态: APPROVED");
        }
        
        // 3. 检查有效期
        LocalDateTime now = LocalDateTime.now();
        if (ticket.getExpiresAt().isBefore(now)) {
            return AuthTicketValidationResult.invalid("授权票据已过期: " + ticket.getExpiresAt());
        }
        
        // 4. 检查目标全宗号
        if (!ticket.getTargetFonds().equals(targetFonds)) {
            return AuthTicketValidationResult.invalid(
                "授权票据目标全宗不匹配，票据目标: " + ticket.getTargetFonds() + 
                "，访问目标: " + targetFonds);
        }
        
        // 5. 检查访问范围
        AuthScope ticketScope;
        try {
            ticketScope = objectMapper.readValue(ticket.getScope(), AuthScope.class);
        } catch (Exception e) {
            log.error("反序列化授权票据访问范围失败: ticketId={}", ticketId, e);
            return AuthTicketValidationResult.invalid("授权票据访问范围格式错误");
        }
        
        if (!isAccessScopeAllowed(ticketScope, accessScope)) {
            return AuthTicketValidationResult.invalid("访问范围超出授权范围");
        }
        
        // 6. 验证通过
        return AuthTicketValidationResult.valid(
            ticket.getId(),
            ticket.getApplicantId(),
            ticket.getSourceFonds(),
            ticket.getTargetFonds(),
            ticket.getExpiresAt()
        );
    }
    
    @Override
    public boolean isAccessScopeAllowed(AuthScope ticketScope, AuthScope accessScope) {
        if (ticketScope == null || accessScope == null) {
            return false;
        }
        
        // 1. 检查归档年度（如果票据指定了年度）
        if (ticketScope.getArchiveYears() != null && !ticketScope.getArchiveYears().isEmpty()) {
            if (accessScope.getArchiveYears() != null && !accessScope.getArchiveYears().isEmpty()) {
                // 访问的年度必须在票据授权的年度范围内
                for (Integer accessYear : accessScope.getArchiveYears()) {
                    if (!ticketScope.getArchiveYears().contains(accessYear)) {
                        return false;
                    }
                }
            } else {
                // 如果访问未指定年度，但票据指定了年度，则不允许
                return false;
            }
        }
        
        // 2. 检查档案类型（如果票据指定了类型）
        if (ticketScope.getDocTypes() != null && !ticketScope.getDocTypes().isEmpty()) {
            if (accessScope.getDocTypes() != null && !accessScope.getDocTypes().isEmpty()) {
                // 访问的类型必须在票据授权的类型范围内
                for (String accessType : accessScope.getDocTypes()) {
                    if (!ticketScope.getDocTypes().contains(accessType)) {
                        return false;
                    }
                }
            } else {
                // 如果访问未指定类型，但票据指定了类型，则不允许
                return false;
            }
        }
        
        // 3. 检查关键词（如果票据指定了关键词）
        if (ticketScope.getKeywords() != null && !ticketScope.getKeywords().isEmpty()) {
            // [P1-FIX] 关键词匹配：使用精确匹配而非 contains，防止授权范围被意外扩大
            if (accessScope.getKeywords() == null || accessScope.getKeywords().isEmpty()) {
                return false;
            }
            // 关键词匹配：访问的关键词必须在票据授权的关键词范围内（精确匹配）
            for (String accessKeyword : accessScope.getKeywords()) {
                if (!ticketScope.getKeywords().contains(accessKeyword)) {
                    return false;
                }
            }
        }
        
        // 4. 检查访问类型（只读/读写）
        if (ticketScope.getAccessType() != null) {
            if ("READ_ONLY".equals(ticketScope.getAccessType())) {
                // 如果票据是只读，访问类型也必须是只读
                if (accessScope.getAccessType() != null && 
                    !"READ_ONLY".equals(accessScope.getAccessType())) {
                    return false;
                }
            }
        }
        
        return true;
    }
}

