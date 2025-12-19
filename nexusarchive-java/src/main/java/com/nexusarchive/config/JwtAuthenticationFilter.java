package com.nexusarchive.config;

import com.nexusarchive.service.CustomUserDetailsService;
import com.nexusarchive.service.TokenBlacklistService;
import com.nexusarchive.util.JwtUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT认证过滤器 (安全重构版本)
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String userId;

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        jwt = authHeader.substring(7);

        try {
            // 1. 检查Token是否在黑名单中
            if (tokenBlacklistService.isBlacklisted(jwt)) {
                // 如果在黑名单中，默默拒绝，不提供任何额外信息
                filterChain.doFilter(request, response);
                return;
            }

            // 2. 验证Token签名和过期时间 (validateToken会抛出异常)
            jwtUtil.validateToken(jwt);
            
            // 3. 提取用户ID，并确保当前没有已认证的用户
            userId = jwtUtil.extractUserId(jwt);
            if (userId != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                
                // 4. 从数据库加载用户信息
                UserDetails userDetails = userDetailsService.loadUserById(userId);

                // 5. 创建认证凭证并设置到SecurityContext中
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                // 将用户ID放入Request属性中，方便后续Controller或Service层获取
                request.setAttribute("userId", userId);
            }

        } catch (SignatureException e) {
            // 这是一个严重的安全事件，签名无效意味着Token可能被篡改或伪造
            logger.error("[SECURITY] Invalid JWT Signature: " + e.getMessage() + " - Token: " + jwt);
        } catch (ExpiredJwtException e) {
            // Token过期是正常情况，记录为警告即可
            logger.warn("Expired JWT Token: " + e.getMessage());
        } catch (MalformedJwtException e) {
            // Token格式错误，可能是客户端问题或攻击尝试
            logger.warn("Malformed JWT Token: " + e.getMessage() + " - Token: " + jwt);
        } catch (JwtException e) {
            // 其他JWT相关异常
            logger.error("JWT processing error: " + e.getMessage());
        } catch (Exception e) {
            // 其他所有意料之外的异常
            logger.error("An unexpected error occurred in JwtAuthenticationFilter", e);
        }

        filterChain.doFilter(request, response);
    }
}
